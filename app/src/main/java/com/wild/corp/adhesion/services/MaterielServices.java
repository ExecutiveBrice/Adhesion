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

    @Transactional(readOnly = true)
    public List<MaterielResponse> getAll() {
        return materielRepository.findAllByOrderByNomAscIdAsc().stream().map(this::toResponse).toList();
    }

    public MaterielResponse create(MaterielRequest request) {
        if (request == null || request.zonageId() == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le zonage du matériel est obligatoire");
        }
        String nom = nomValide(request.nom(), 150, "matériel");
        Zonage zonage = zonage(request.zonageId());
        return toResponse(materielRepository.save(Materiel.builder().nom(nom).zonage(zonage)
                .horsService(Boolean.TRUE.equals(request.horsService())).build()));
    }

    public MaterielResponse update(Long id, MaterielRequest request) {
        if (request == null) throw erreur(HttpStatus.BAD_REQUEST, "Le matériel est obligatoire");
        String nom = nomValide(request.nom(), 150, "matériel");
        if (request.zonageId() == null) throw erreur(HttpStatus.BAD_REQUEST, "Le zonage est obligatoire");
        Zonage zonage = zonage(request.zonageId());
        Materiel materiel = materiel(id);
        materiel.setNom(nom);
        materiel.setZonage(zonage);
        return toResponse(materiel);
    }

    public void delete(Long id) {
        Materiel materiel = materiel(id);
        if (materiel.getEmprunteurId() != null) {
            throw erreur(HttpStatus.CONFLICT, "Ce matériel est emprunté : restituez-le avant de le supprimer");
        }
        empruntRepository.deleteAllByMaterielId(id);
        materielRepository.delete(materiel);
    }

    public MaterielResponse emprunter(Long id, String username) {
        return emprunter(id, username, null, false);
    }

    public MaterielResponse emprunter(Long id, String username, Long emprunteurId, boolean administrateur) {
        Materiel materiel = materiel(id);
        if (materiel.getEmprunteurId() != null) {
            throw erreur(HttpStatus.CONFLICT, "Ce matériel est déjà emprunté");
        }
        User acteur = utilisateur(username);
        if (emprunteurId != null && !administrateur) {
            throw erreur(HttpStatus.FORBIDDEN, "Seul un administrateur peut choisir l’emprunteur");
        }
        User emprunteur = emprunteurId == null ? acteur : userRepository.findById(emprunteurId)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Membre du CA introuvable"));
        if (administrateur && !emprunteur.getRoles().contains(ERole.ROLE_MEMBRECA)) {
            throw erreur(HttpStatus.BAD_REQUEST, "L’emprunteur sélectionné doit être un membre du conseil d’administration");
        }
        materiel.setEmprunteurId(emprunteur.getId());
        materiel.setEmprunteurNom(nomUtilisateur(emprunteur));
        materiel.setDateEmprunt(OffsetDateTime.now(ZoneOffset.UTC));
        EmpruntMateriel emprunt = nouvelEmprunt(materiel);
        emprunt.setEnregistreParId(acteur.getId());
        emprunt.setEnregistreParNom(nomUtilisateur(acteur));
        empruntRepository.save(emprunt);
        return toResponse(materiel);
    }

    public MaterielResponse restituer(Long id, String username, boolean administrateur) {
        Materiel materiel = materiel(id);
        if (materiel.getEmprunteurId() == null) {
            throw erreur(HttpStatus.CONFLICT, "Ce matériel est déjà disponible");
        }
        User acteur = utilisateur(username);
        if (!administrateur && !materiel.getEmprunteurId().equals(acteur.getId())) {
            throw erreur(HttpStatus.FORBIDDEN, "Seul l’emprunteur ou un administrateur peut restituer ce matériel");
        }
        EmpruntMateriel emprunt = empruntOuExistant(materiel);
        emprunt.setDateRetour(OffsetDateTime.now(ZoneOffset.UTC));
        emprunt.setRapporteParId(acteur.getId());
        emprunt.setRapporteParNom(nomUtilisateur(acteur));
        materiel.setEmprunteurId(null);
        materiel.setEmprunteurNom(null);
        materiel.setDateEmprunt(null);
        return toResponse(materiel);
    }

    public MaterielResponse updateHorsService(Long id, HorsServiceRequest request, String username) {
        if (request == null || request.horsService() == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le statut hors service est obligatoire");
        }
        Materiel materiel = materiel(id);
        if (materiel.isHorsService() == request.horsService()) return toResponse(materiel);
        String commentaire = request.commentaire() == null ? "" : request.commentaire().trim();
        if (commentaire.isBlank() || commentaire.length() > 2000) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le problème ou la réparation est obligatoire et limité à 2000 caractères");
        }
        User acteur = utilisateur(username);
        // Reprendre un éventuel emprunt ancien avant le nouvel événement du journal.
        if (materiel.getEmprunteurId() != null) empruntOuExistant(materiel);
        empruntRepository.save(EmpruntMateriel.builder().materiel(materiel)
                .horsService(request.horsService()).commentaire(commentaire).dateEmprunt(OffsetDateTime.now(ZoneOffset.UTC))
                .enregistreParId(acteur.getId()).enregistreParNom(nomUtilisateur(acteur)).build());
        materiel.setHorsService(request.horsService());
        materiel.setProbleme(request.horsService() ? commentaire : null);
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
                .map(emprunt -> new EmpruntMaterielResponse(emprunt.getId(), emprunt.getEmprunteurId(),
                        emprunt.getEmprunteurNom(), emprunt.getDateEmprunt(), emprunt.getDateRetour(),
                        emprunt.getEnregistreParNom(), emprunt.getRapporteParNom(), emprunt.getHorsService(), emprunt.getCommentaire())).toList();
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
        return materielRepository.findByIdForUpdate(id)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Matériel introuvable"));
    }

    private User utilisateur(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> erreur(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private ZonageConfiguration toConfiguration(Zonage zonage) {
        return new ZonageConfiguration(zonage.getId(), zonage.getNom(), zonage.getCouleur());
    }

    private MaterielResponse toResponse(Materiel materiel) {
        return new MaterielResponse(materiel.getId(), materiel.getNom(), toConfiguration(materiel.getZonage()),
                materiel.getEmprunteurId(), materiel.getEmprunteurNom(), materiel.getDateEmprunt(), materiel.isHorsService(), materiel.getProbleme());
    }

    private ResponseStatusException erreur(HttpStatus statut, String message) {
        return new ResponseStatusException(statut, message);
    }
}
