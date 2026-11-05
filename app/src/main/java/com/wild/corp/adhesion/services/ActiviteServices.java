package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse;
import com.wild.corp.adhesion.models.resources.SeanceCalendrierResponse;
import com.wild.corp.adhesion.repository.ActiviteNm1Repository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.SalleRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.SectionListingSpecifications;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActiviteServices {

    private static final Sort ACTIVITE_SORT = Sort.by(Sort.Direction.ASC, "nom", "id");
    private static final List<String> STATUTS_REFERENTS_EXCLUS = List.of(
            Status.ANNULEE.label, Status.LISTE_ATTENTE.label);

    @Autowired
    ActiviteRepository activiteRepository;

    @Autowired
    AdherentServices adherentServices;
    @Autowired
    AdherentRepository adherentRepository;
    @Autowired
    ActiviteNm1Repository activiteNm1Repository;
    @Autowired
    ActivityTeamService activityTeams;
    @Autowired
    SeanceRepository seanceRepository;
    @Autowired
    ResponsabiliteSeanceServices responsabiliteSeanceServices;
    @Autowired
    SalleRepository salleRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    com.wild.corp.adhesion.repository.UserRepository userRepository;

    @Autowired
    com.wild.corp.adhesion.repository.EquipeRepository equipeRepository;

    public List<ActiviteNm1> getAllNm1() {
        List<ActiviteNm1> activites = activiteNm1Repository.findAll();

        List<ActiviteNm1> uniques = activites.stream()
                .collect(Collectors.toMap(
                        ActiviteNm1::getActiviteId,
                        a -> a,
                        (a1, a2) -> a1
                ))
                .values()
                .stream()
                .toList();

        return uniques;
    }

    public List<Activite> getAll() {
        List<Activite> activites = activiteRepository.findAll();
        activites.forEach(this::completerCompteurs);
        return activites;
    }

    public Activite getDetail(Long activiteId) {
        return completerCompteurs(activiteRepository.findById(activiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable")));
    }

    public Activite getManagedDetail(String username, Long activiteId) {
        return completerCompteurs(requireManagedActivity(username, activiteId));
    }

    public Page<Activite> getPage(String search, Integer tarif, Boolean complete, Boolean reinscription,
                                  Integer age, String genre, Pageable pageable) {
        return getPage(search, tarif, complete, reinscription, age, genre, pageable, null);
    }

    public Page<Activite> getManagedPage(String username, String search, Integer tarif, Boolean complete,
                                         Boolean reinscription, Integer age, String genre, Pageable pageable) {
        Set<Long> sectionIds = sectionRepository.findManagedByUsername(username).stream()
                .map(Section::getId).collect(Collectors.toSet());
        boolean encadrant = userRepository.findByUsername(username)
                .map(user -> user.getRoles().contains(ERole.ROLE_ENCADRANT)).orElse(false);
        if (sectionIds.isEmpty() && !encadrant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucune section ne vous est confiée");
        }
        Specification<Activite> scope = SectionListingSpecifications.activites(sectionIds);
        if (encadrant) scope = scope.or(SectionListingSpecifications.activitesEncadrees(username));
        return getPage(search, tarif, complete, reinscription, age, genre, pageable, scope);
    }

    private Page<Activite> getPage(String search, Integer tarif, Boolean complete, Boolean reinscription,
                                  Integer age, String genre, Pageable pageable, Specification<Activite> scope) {
        Pageable activitePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ACTIVITE_SORT);
        Specification<Activite> filters = specificationFiltre(search, tarif, complete, reinscription, age, genre);
        if (scope != null) {
            filters = filters.and(scope);
        }
        Page<Activite> activites;
        if (scope != null || StringUtils.hasText(search) || tarif != null || complete != null
                || reinscription != null || age != null || StringUtils.hasText(genre)) {
            activites = activiteRepository.findAll(filters, activitePageable);
        } else {
            activites = activiteRepository.findAll(activitePageable);
        }
        return activites.map(this::completerCompteurs);
    }

    public Activite saveManaged(String username, Activite activite) {
        Set<Long> allowedSectionIds = managedSectionIds(username);
        if (activite == null || activite.getSection() == null || activite.getSection().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une section existante");
        }
        Long sectionId = activite.getSection().getId();
        if (!allowedSectionIds.contains(sectionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette section ne vous est pas confiée");
        }
        if (activite.getId() != null) {
            Activite existing = activiteRepository.findForTeamUpdate(activite.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
            if (existing.getSection() == null || !sectionId.equals(existing.getSection().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’activité n’appartient pas à cette section");
            }
        }
        return save(activite);
    }

    public record EncadrantOption(Long id, String prenom, String nom) { }

    public List<EncadrantOption> getManagedEncadrants(String username) {
        managedSectionIds(username);
        return adherentRepository.findByUserRole(ERole.ROLE_ENCADRANT).stream()
                .map(adherent -> new EncadrantOption(adherent.getId(), adherent.getPrenom(), adherent.getNom()))
                .sorted(Comparator.comparing(EncadrantOption::nom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(EncadrantOption::prenom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<AdherentResponsabiliteResponse> getManagedResponsabiliteCandidates(String username, Long activiteId) {
        requireManagedActivity(username, activiteId);
        return responsabiliteSeanceServices.getCandidats(activiteId);
    }

    private Activite requireManagedActivity(String username, Long activiteId) {
        Set<Long> allowedSectionIds = managedSectionIds(username);
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
        if (activite.getSection() == null || !allowedSectionIds.contains(activite.getSection().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette activité ne vous est pas confiée");
        }
        return activite;
    }

    private Set<Long> managedSectionIds(String username) {
        Set<Long> sectionIds = sectionRepository.findManagedByUsername(username).stream()
                .map(Section::getId).collect(Collectors.toSet());
        if (sectionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucune section ne vous est confiée");
        }
        return sectionIds;
    }

    private Specification<Activite> specificationFiltre(String recherche, Integer tarif, Boolean complete,
                                                          Boolean reinscription, Integer age, String genre) {
        return (root, query, criteriaBuilder) -> {
            jakarta.persistence.criteria.Predicate filtre = criteriaBuilder.conjunction();
            if (StringUtils.hasText(recherche)) {
                String rechercheLike = toLikePattern(recherche);
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nom")), rechercheLike, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.join("section", JoinType.LEFT).get("nom")), rechercheLike, '\\')
                ));
            }
            if (tarif != null) {
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.equal(root.get("tarif"), tarif));
            }
            if (complete != null) {
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.equal(root.get("complete"), complete));
            }
            if (reinscription != null) {
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.equal(root.get("reinscription"), reinscription));
            }
            if (age != null) {
                filtre = criteriaBuilder.and(filtre,
                        criteriaBuilder.lessThanOrEqualTo(root.get("ageMin"), age),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("ageMax"), age));
            }
            if (StringUtils.hasText(genre)) {
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.equal(root.get("genre"), genre));
            }
            return filtre;
        };
    }

    private String toLikePattern(String valeur) {
        String valeurEchappee = valeur.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + valeurEchappee + "%";
    }

    private Activite completerCompteurs(Activite activite) {
        var debutSemaine = LocalDate.now(ZoneId.of("Europe/Paris"))
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        var finSemaine = debutSemaine.plusWeeks(1);
        activite.setEvenementsSemaineProchaine(activite.getSeances().stream()
                .filter(seance -> seance.getDebut() != null && !seance.getDebut().isBefore(debutSemaine)
                        && seance.getDebut().isBefore(finSemaine))
                .filter(seance -> !ESeance.ANNULEE.equals(seance.getEtatSeance()))
                .sorted(Comparator.comparing(Seance::getDebut).thenComparing(Seance::getId))
                .map(SeanceCalendrierResponse::from).toList());
        activite.setEquipes(equipeRepository.findByActivite_IdOrderByIdAsc(activite.getId()).stream()
                .filter(team -> !team.isArchivee())
                .map(team -> new Activite.EquipeResume(team.getId(), team.getNom(), team.getAffectations().stream()
                        .filter(a -> TeamRoster.active(a, LocalDate.now()) && TeamRoster.eligible(a.getAdhesion(), activite))
                        .map(a -> a.getAdhesion().getAdherent().getId()).distinct().count())).toList());
        activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(Adhesion::isValide).count());
        activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(Adhesion::isEnCours).count());
        activite.setNbAdhesionsAttente(activite.getAdhesions().stream().filter(Adhesion::isEnAttente).count());
        activite.setNbEvenementsRealises(activite.getSeances().stream()
                .filter(seance -> ESeance.REALISEE.equals(seance.getEtatSeance()))
                .count());
        activite.setNbEvenementsTotal(activite.getSeances().stream()
                .filter(seance -> !ESeance.ANNULEE.equals(seance.getEtatSeance()))
                .count());
        return activite;
    }

    public List<com.wild.corp.adhesion.models.resources.AdherentLite> getReferentsCandidates(Long activiteId) {
        Activite activite = getById(activiteId);
        return adherentServices.getLites(new LinkedHashSet<>(adherentRepository.findReferentCandidates(
                activite.getSection().getId(), STATUTS_REFERENTS_EXCLUS)));
    }

    public Activite addReferent(Long activiteId, Long adherentId) {
        Activite activiteDB = activiteRepository.findById(activiteId).get();
        Adherent adherent = adherentServices.getById(adherentId);
        activiteDB.getProfs().add(adherent);
        adherent.getCours().add(activiteDB);

        return activiteRepository.save(activiteDB);
    }

    public List<Activite> findByNom(String nom) {
        return activiteRepository.findByNom(nom);
    }

    public List<ActiviteNm1> findNm1ByNom(String nom) {
        return activiteNm1Repository.findByNom(nom);
    }

    public Activite getById(Long activiteId) {
        return activiteRepository.findById(activiteId).get();
    }
    public List<ActiviteNm1> getNm1ById(Long activiteId) {
        return activiteNm1Repository.findByActiviteId(activiteId);
    }

    public Activite save(Activite activite) {
        if (activite == null || activite.getSection() == null || activite.getSection().getId() == null || activite.getSection().getId() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une section existante");
        activite.setSection(sectionRepository.findById(activite.getSection().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section introuvable")));
        Activite target = activite;
        if (activite.getId() != null) {
            target = activiteRepository.findForTeamUpdate(activite.getId()).orElseThrow();
            BeanUtils.copyProperties(activite, target, "id", "adhesions", "sousClassement", "profs", "referents", "seances",
                    "nbAdhesionsEnCours", "nbAdhesionsCompletes", "nbAdhesionsAttente", "nbEvenementsRealises", "nbEvenementsTotal", "montantCollecte", "equipes", "evenementsSemaineProchaine");
        }
        target = activiteRepository.saveAndFlush(target);
        activityTeams.ensureFirstTeam(target);
        return target;
    }

}
