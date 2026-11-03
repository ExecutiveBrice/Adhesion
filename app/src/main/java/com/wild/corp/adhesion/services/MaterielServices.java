package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Transactional
public class MaterielServices {
    private final MaterielRepository materielRepository;
    private final ZonageRepository zonageRepository;
    private final UserRepository userRepository;
    private final EmpruntMaterielRepository empruntRepository;

    public MaterielServices(MaterielRepository materielRepository, ZonageRepository zonageRepository,
            UserRepository userRepository, EmpruntMaterielRepository empruntRepository) {
        this.materielRepository = materielRepository;
        this.zonageRepository = zonageRepository;
        this.userRepository = userRepository;
        this.empruntRepository = empruntRepository;
    }

    public List<MaterielResponse> getAll() {
        return materielRepository.findAllByOrderByNomAscIdAsc().stream().map(m -> toResponse(m.getEmprunteurId() == null ? m : materiel(m.getId()))).toList();
    }

    public MaterielResponse create(MaterielRequest request) {
        if (request == null || request.zonageId() == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le zonage du matériel est obligatoire");
        }
        String nom = nomValide(request.nom(), 150, "matériel");
        Zonage zonage = zonage(request.zonageId());
        return toResponse(materielRepository.save(Materiel.builder().nom(nom).zonage(zonage)
                .quantiteTotale(quantitePositive(request.quantiteTotale() == null ? 1 : request.quantiteTotale()))
                .quantiteHorsService(Boolean.TRUE.equals(request.horsService()) ? (request.quantiteTotale() == null ? 1 : request.quantiteTotale()) : 0)
                .horsService(Boolean.TRUE.equals(request.horsService())).build()));
    }

    public MaterielResponse update(Long id, MaterielRequest request) {
        if (request == null) throw erreur(HttpStatus.BAD_REQUEST, "Le matériel est obligatoire");
        String nom = nomValide(request.nom(), 150, "matériel");
        if (request.zonageId() == null) throw erreur(HttpStatus.BAD_REQUEST, "Le zonage est obligatoire");
        Zonage zonage = zonage(request.zonageId());
        Materiel materiel = materiel(id);
        int total = quantitePositive(request.quantiteTotale() == null ? materiel.getQuantiteTotale() : request.quantiteTotale());
        if (total < quantiteEmpruntee(materiel) + materiel.getQuantiteHorsService())
            throw erreur(HttpStatus.CONFLICT, "Le stock ne peut pas être inférieur aux quantités empruntées et hors service");
        materiel.setQuantiteTotale(total);
        materiel.setNom(nom);
        materiel.setZonage(zonage);
        return toResponse(materiel);
    }

    public void delete(Long id) {
        Materiel materiel = materiel(id);
        if (quantiteEmpruntee(materiel) > 0) {
            throw erreur(HttpStatus.CONFLICT, "Ce matériel est emprunté : restituez-le avant de le supprimer");
        }
        empruntRepository.deleteAllByMaterielId(id);
        materielRepository.delete(materiel);
    }

    public MaterielResponse emprunter(Long id, String username) {
        return emprunter(id, username, null, false);
    }

    public MaterielResponse emprunter(Long id, String username, Long emprunteurId, boolean administrateur) {
        return emprunter(id, username, emprunteurId, administrateur, 1);
    }

    public MaterielResponse emprunter(Long id, String username, Long emprunteurId, boolean administrateur, Integer quantite) {
        Materiel materiel = materiel(id);
        int nombre = quantitePositive(quantite == null ? 1 : quantite);
        if (nombre > materiel.getQuantiteTotale() - materiel.getQuantiteHorsService() - quantiteEmpruntee(materiel))
            throw erreur(HttpStatus.CONFLICT, "Quantité disponible insuffisante");
        User acteur = utilisateur(username);
        if (emprunteurId != null && !administrateur) {
            throw erreur(HttpStatus.FORBIDDEN, "Seul un administrateur peut choisir l’emprunteur");
        }
        User emprunteur = emprunteurId == null ? acteur : userRepository.findById(emprunteurId)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Membre du CA introuvable"));
        if (administrateur && !emprunteur.getRoles().contains(ERole.ROLE_MEMBRECA)) {
            throw erreur(HttpStatus.BAD_REQUEST, "L’emprunteur sélectionné doit être un membre du conseil d’administration");
        }
        EmpruntMateriel emprunt = EmpruntMateriel.builder().materiel(materiel).quantite(nombre)
                .emprunteurId(emprunteur.getId()).emprunteurNom(nomUtilisateur(emprunteur))
                .dateEmprunt(OffsetDateTime.now(ZoneOffset.UTC)).build();
        emprunt.setEnregistreParId(acteur.getId());
        emprunt.setEnregistreParNom(nomUtilisateur(acteur));
        empruntRepository.save(emprunt);
        return toResponse(materiel);
    }

    public MaterielResponse restituer(Long id, String username, boolean administrateur) {
        return restituer(id, username, administrateur, null);
    }

    public MaterielResponse restituer(Long id, String username, boolean administrateur, RetourMaterielRequest request) {
        Materiel materiel = materiel(id);
        User acteur = utilisateur(username);
        List<EmpruntMateriel> ouverts = ouverts(materiel);
        EmpruntMateriel emprunt;
        if (request == null || request.empruntId() == null) {
            List<EmpruntMateriel> candidats = ouverts.stream()
                    .filter(e -> administrateur || Objects.equals(e.getEmprunteurId(), acteur.getId())).toList();
            if (!administrateur && candidats.isEmpty() && !ouverts.isEmpty())
                throw erreur(HttpStatus.FORBIDDEN, "Seul l’emprunteur ou un administrateur peut restituer ce matériel");
            if (candidats.size() != 1) throw erreur(HttpStatus.CONFLICT, "Sélectionnez l’emprunt à restituer");
            emprunt = candidats.get(0);
        } else {
            emprunt = ouverts.stream().filter(e -> e.getId().equals(request.empruntId())).findFirst()
                    .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "Emprunt introuvable ou déjà restitué"));
        }
        if (!administrateur && !Objects.equals(emprunt.getEmprunteurId(), acteur.getId()))
            throw erreur(HttpStatus.FORBIDDEN, "Seul l’emprunteur ou un administrateur peut restituer ce matériel");
        int restant = emprunt.getQuantite() - emprunt.getQuantiteRetournee();
        int nombre = quantitePositive(request == null || request.quantite() == null ? restant : request.quantite());
        if (nombre > restant) throw erreur(HttpStatus.CONFLICT, "La quantité dépasse le reste à rendre");
        OffsetDateTime maintenant = OffsetDateTime.now(ZoneOffset.UTC);
        emprunt.setQuantiteRetournee(emprunt.getQuantiteRetournee() + nombre);
        emprunt.getRetours().add(new RetourMateriel(nombre, maintenant, acteur.getId(), nomUtilisateur(acteur)));
        if (nombre == restant) emprunt.setDateRetour(maintenant);
        emprunt.setRapporteParId(acteur.getId());
        emprunt.setRapporteParNom(nomUtilisateur(acteur));
        return toResponse(materiel);
    }

    public MaterielResponse updateHorsService(Long id, HorsServiceRequest request, String username) {
        if (request == null || request.horsService() == null && request.quantiteHorsService() == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le statut hors service est obligatoire");
        }
        Materiel materiel = materiel(id);
        int nombre = request.quantiteHorsService() == null ? (Boolean.TRUE.equals(request.horsService()) ? materiel.getQuantiteTotale() : 0) : request.quantiteHorsService();
        if (nombre < 0 || nombre > materiel.getQuantiteTotale() - quantiteEmpruntee(materiel))
            throw erreur(HttpStatus.CONFLICT, "La quantité hors service dépasse le stock non emprunté");
        if (nombre == materiel.getQuantiteHorsService()) return toResponse(materiel);
        String commentaire = request.commentaire() == null ? "" : request.commentaire().trim();
        if (commentaire.isBlank() || commentaire.length() > 2000) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le problème ou la réparation est obligatoire et limité à 2000 caractères");
        }
        User acteur = utilisateur(username);
        // Reprendre un éventuel emprunt ancien avant le nouvel événement du journal.
        if (materiel.getEmprunteurId() != null) empruntOuExistant(materiel);
        empruntRepository.save(EmpruntMateriel.builder().materiel(materiel)
                .horsService(nombre > materiel.getQuantiteHorsService()).quantite(Math.abs(nombre - materiel.getQuantiteHorsService())).commentaire(commentaire).dateEmprunt(OffsetDateTime.now(ZoneOffset.UTC))
                .enregistreParId(acteur.getId()).enregistreParNom(nomUtilisateur(acteur)).build());
        materiel.setQuantiteHorsService(nombre);
        materiel.setHorsService(nombre > 0);
        materiel.setProbleme(nombre > 0 ? commentaire : null);
        return toResponse(materiel);
    }

    @Transactional(readOnly = true)
    public List<MembreCAMaterielResponse> getMembresCA() {
        return userRepository.findAllByRole(ERole.ROLE_MEMBRECA).stream()
                .map(user -> new MembreCAMaterielResponse(user.getId(), nomUtilisateur(user)))
                .sorted(java.util.Comparator.comparing(MembreCAMaterielResponse::nom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<EmpruntMaterielResponse> getHistorique(Long id) {
        Materiel materiel = materiel(id);
        // Préserve aussi les emprunts encore ouverts datant d'avant la création du journal.
        if (materiel.getEmprunteurId() != null) empruntOuExistant(materiel);
        return empruntRepository.findAllByMaterielIdOrderByDateEmpruntDescIdDesc(id).stream()
                .map(this::toEmpruntResponse).toList();
    }

    private EmpruntMaterielResponse toEmpruntResponse(EmpruntMateriel emprunt) {
        return new EmpruntMaterielResponse(emprunt.getId(), emprunt.getEmprunteurId(), emprunt.getEmprunteurNom(),
                emprunt.getDateEmprunt(), emprunt.getDateRetour(), emprunt.getEnregistreParNom(), emprunt.getRapporteParNom(),
                emprunt.getHorsService(), emprunt.getCommentaire(), emprunt.getQuantite(), emprunt.getQuantiteRetournee(), List.copyOf(emprunt.getRetours()));
    }

    private List<EmpruntMateriel> ouverts(Materiel materiel) {
        return empruntRepository.findAllByMaterielIdOrderByDateEmpruntDescIdDesc(materiel.getId()).stream()
                .filter(e -> e.getHorsService() == null && e.getDateRetour() == null).toList();
    }

    private int quantiteEmpruntee(Materiel materiel) {
        return ouverts(materiel).stream().mapToInt(e -> e.getQuantite() - e.getQuantiteRetournee()).sum();
    }

    private int quantitePositive(int quantite) {
        if (quantite < 1) throw erreur(HttpStatus.BAD_REQUEST, "La quantité doit être un entier positif");
        return quantite;
    }

    private EmpruntMateriel empruntOuExistant(Materiel materiel) {
        return empruntRepository.findFirstByMaterielIdAndHorsServiceIsNullAndDateRetourIsNullOrderByDateEmpruntDescIdDesc(materiel.getId())
                .orElseGet(() -> empruntRepository.save(nouvelEmprunt(materiel)));
    }

    private EmpruntMateriel nouvelEmprunt(Materiel materiel) {
        return EmpruntMateriel.builder().materiel(materiel).emprunteurId(materiel.getEmprunteurId())
                .emprunteurNom(materiel.getEmprunteurNom()).dateEmprunt(materiel.getDateEmprunt()).build();
    }

    private String nomUtilisateur(User user) {
        String nom = user.getAdherent() == null ? "" : Stream.of(user.getAdherent().getPrenom(), user.getAdherent().getNom())
                .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        return nom.isBlank() ? user.getUsername() : nom;
    }

    @Transactional(readOnly = true)
    public List<ZonageConfiguration> getZonages() {
        return zonageRepository.findAllByOrderByNomAscIdAsc().stream().map(this::toConfiguration).toList();
    }

    public ZonageConfiguration createZonage(ZonageConfiguration request) {
        ZonageConfiguration configuration = normaliser(request);
        return toConfiguration(zonageRepository.save(Zonage.builder()
                .nom(configuration.nom()).couleur(configuration.couleur()).build()));
    }

    public ZonageConfiguration updateZonage(Long id, ZonageConfiguration request) {
        ZonageConfiguration configuration = normaliser(request);
        Zonage zonage = zonage(id);
        zonage.setNom(configuration.nom());
        zonage.setCouleur(configuration.couleur());
        return toConfiguration(zonage);
    }

    public void deleteZonage(Long id) {
        Zonage zonage = zonage(id);
        if (materielRepository.existsByZonageId(id)) {
            throw erreur(HttpStatus.CONFLICT, "Ce zonage contient du matériel et ne peut pas être supprimé");
        }
        zonageRepository.delete(zonage);
    }

    private ZonageConfiguration normaliser(ZonageConfiguration request) {
        if (request == null) throw erreur(HttpStatus.BAD_REQUEST, "Le zonage est obligatoire");
        String nom = nomValide(request.nom(), 100, "zonage");
        if (request.couleur() == null || !request.couleur().matches("#[0-9a-fA-F]{6}")) {
            throw erreur(HttpStatus.BAD_REQUEST, "La couleur doit être au format hexadécimal (#RRGGBB)");
        }
        return new ZonageConfiguration(null, nom, request.couleur().toUpperCase(Locale.ROOT));
    }

    private String nomValide(String valeur, int limite, String objet) {
        String nom = valeur == null ? "" : valeur.trim();
        if (nom.isBlank() || nom.length() > limite) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le nom du " + objet + " est obligatoire et limité à " + limite + " caractères");
        }
        return nom;
    }

    private Zonage zonage(Long id) {
        return zonageRepository.findByIdForUpdate(id)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Zonage introuvable"));
    }

    private Materiel materiel(Long id) {
        Materiel materiel = materielRepository.findByIdForUpdate(id)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Matériel introuvable"));
        migrerAncienEmprunt(materiel);
        return materiel;
    }

    private User utilisateur(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> erreur(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private ZonageConfiguration toConfiguration(Zonage zonage) {
        return new ZonageConfiguration(zonage.getId(), zonage.getNom(), zonage.getCouleur());
    }

    private void migrerAncienEmprunt(Materiel materiel) {
        if (materiel.getEmprunteurId() != null) {
            empruntOuExistant(materiel);
            materiel.setEmprunteurId(null);
            materiel.setEmprunteurNom(null);
            materiel.setDateEmprunt(null);
        }
    }

    private MaterielResponse toResponse(Materiel materiel) {
        List<EmpruntMaterielResponse> emprunts = ouverts(materiel).stream().map(this::toEmpruntResponse).toList();
        int empruntee = emprunts.stream().mapToInt(e -> e.quantite() - e.quantiteRetournee()).sum();
        EmpruntMaterielResponse premier = emprunts.isEmpty() ? null : emprunts.get(0);
        return new MaterielResponse(materiel.getId(), materiel.getNom(), toConfiguration(materiel.getZonage()),
                premier == null ? null : premier.emprunteurId(), premier == null ? null : premier.emprunteurNom(),
                premier == null ? null : premier.dateEmprunt(), materiel.getQuantiteHorsService() > 0, materiel.getProbleme(),
                materiel.getQuantiteTotale(), materiel.getQuantiteHorsService(), empruntee,
                materiel.getQuantiteTotale() - materiel.getQuantiteHorsService() - empruntee, emprunts);
    }

    private ResponseStatusException erreur(HttpStatus statut, String message) {
        return new ResponseStatusException(statut, message);
    }
}
