package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.repository.ActiviteNm1Repository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.SalleRepository;
import com.wild.corp.adhesion.repository.RoleRepository;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Transactional
public class ActiviteServices {

    private static final Sort ACTIVITE_SORT = Sort.by(Sort.Direction.ASC, "groupeFiltre", "nom", "horaire");

    @Autowired
    ActiviteRepository activiteRepository;

    @Autowired
    AdherentServices adherentServices;
    @Autowired
    ActiviteNm1Repository activiteNm1Repository;
    @Autowired
    SeanceServices seanceServices;
    @Autowired
    SalleRepository salleRepository;
    @Autowired
    RoleRepository roleRepository;

    public List<Seance> getSeancesDuJour(Long activiteId) {

        Activite activite = getById(activiteId);
        LocalDate now = LocalDate.now();
        return activite.getSeances().stream().filter(seance -> now.equals(seance.getDebut())).toList();
    }

    public List<SeanceResponse> getSeances(Long activiteId) {
        return getById(activiteId).getSeances().stream()
                .sorted(Comparator.comparing(Seance::getDebut, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(SeanceResponse::from)
                .toList();
    }

    public List<SeanceResponse> addSeances(Long activiteId, int nombreSeances, LocalDate dateDebut) {
        Activite activite = getById(activiteId);
        seanceServices.addSeances(activite, nombreSeances, dateDebut);
        activiteRepository.save(activite);
        return getSeances(activiteId);
    }

    public List<SeanceResponse> addSeances(Long activiteId, Long planificationId, int nombreSemaines,
                                            LocalDate dateDebut) {
        Activite activite = getById(activiteId);
        PlanificationHebdomadaire planification = activite.getPlanificationsHebdomadaires().stream()
                .filter(element -> planificationId.equals(element.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La planification sélectionnée est introuvable"));
        seanceServices.addSeances(activite, planification, nombreSemaines, dateDebut);
        activiteRepository.save(activite);
        return getSeances(activiteId);
    }
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

    public Page<Activite> getPage(String search, Integer tarif, Boolean complete, Boolean reinscription,
                                  Integer age, String genre, Pageable pageable) {
        Pageable activitePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ACTIVITE_SORT);
        Page<Activite> activites;
        if (StringUtils.hasText(search) || tarif != null || complete != null || reinscription != null || age != null
                || StringUtils.hasText(genre)) {
            activites = activiteRepository.findAll(specificationFiltre(search, tarif, complete, reinscription, age,
                    genre), activitePageable);
        } else {
            activites = activiteRepository.findAll(activitePageable);
        }
        return activites.map(this::completerCompteurs);
    }

    private Specification<Activite> specificationFiltre(String recherche, Integer tarif, Boolean complete,
                                                          Boolean reinscription, Integer age, String genre) {
        return (root, query, criteriaBuilder) -> {
            jakarta.persistence.criteria.Predicate filtre = criteriaBuilder.conjunction();
            if (StringUtils.hasText(recherche)) {
                String rechercheLike = toLikePattern(recherche);
                filtre = criteriaBuilder.and(filtre, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nom")), rechercheLike, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("groupeFiltre")), rechercheLike, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("horaire")), rechercheLike, '\\')
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
        activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(Adhesion::isValide).count());
        activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(Adhesion::isEnCours).count());
        activite.setNbAdhesionsAttente(activite.getAdhesions().stream().filter(Adhesion::isEnAttente).count());
        activite.setNbSeancesRealisees(activite.getSeances().stream()
                .filter(seance -> ESeance.REALISEE.equals(seance.getEtatSeance()))
                .count());
        activite.setNbSeancesTotal(activite.getSeances().stream()
                .filter(seance -> !ESeance.ANNULEE.equals(seance.getEtatSeance()))
                .count());
        return activite;
    }

    public Activite save(Activite activite) {
        List<PlanificationHebdomadaire> planifications = normaliserPlanifications(activite);
        if (planifications.isEmpty()) {
            if (activite.getPlanificationsHebdomadaires() != null && !activite.getPlanificationsHebdomadaires().isEmpty()) {
                activite.setJour(null);
                activite.setHoraireDebut(null);
                activite.setDuree(null);
                activite.setSalle(null);
            } else {
                activite.setSalle(trouverSalle(activite.getSalle()));
            }
        }
        synchroniserChampsHistoriques(activite, planifications);
        if (activite.getId() != null) {
            Activite activiteInDB = activiteRepository.findById(activite.getId()).orElseThrow();
            Set<Adherent> anciensReferents = new HashSet<>(activiteInDB.getReferents());
            activiteInDB.getProfs().forEach(adherent -> adherent.getCours().remove(activiteInDB));
            activiteInDB.getReferents().forEach(adherent -> adherent.getActivitesReferent().remove(activiteInDB));

            BeanUtils.copyProperties(activite, activiteInDB,
                    "id", "adhesions", "sousClassement", "profs", "referents", "seances",
                    "nbAdhesionsEnCours", "nbAdhesionsCompletes", "nbAdhesionsAttente", "nbSeancesRealisees",
                    "nbSeancesTotal", "montantCollecte", "salle", "planificationsHebdomadaires");
            activiteInDB.setSalle(activite.getSalle());
            remplacerPlanifications(activiteInDB, planifications);

            activiteInDB.setProfs(activite.getProfs().stream()
                    .map(adherent -> adherentServices.getById(adherent.getId()))
                    .collect(Collectors.toSet()));
            activiteInDB.getProfs().forEach(adherent -> adherent.getCours().add(activiteInDB));

            activiteInDB.setReferents(resoudreReferentsValides(activiteInDB, activite.getReferents()));
            activiteInDB.getReferents().forEach(adherent -> adherent.getActivitesReferent().add(activiteInDB));
            activiteInDB.getReferents().forEach(this::ajouterRoleReferent);
            anciensReferents.stream()
                    .filter(adherent -> !activiteInDB.getReferents().contains(adherent))
                    .filter(adherent -> adherent.getActivitesReferent().isEmpty())
                    .forEach(this::retirerRoleReferent);

            return activiteRepository.save(activiteInDB);
        }

        remplacerPlanifications(activite, planifications);
        activite.setProfs(activite.getProfs().stream()
                .map(adherent -> adherentServices.getById(adherent.getId()))
                .collect(Collectors.toSet()));
        activite.getProfs().forEach(adherent -> adherent.getCours().add(activite));
        if (!activite.getReferents().isEmpty()) {
            throw new IllegalArgumentException("Les référents peuvent être ajoutés après l'enregistrement de l'activité");
        }
        if (!planifications.isEmpty()) {
            seanceServices.fillSeances(activite, 29);
        }
        return activiteRepository.save(activite);
    }

    private List<PlanificationHebdomadaire> normaliserPlanifications(Activite activite) {
        List<PlanificationHebdomadaire> planifications = activite.getPlanificationsHebdomadaires();
        if (planifications == null) {
            return List.of();
        }

        List<PlanificationHebdomadaire> planificationsNormalisees = planifications.stream()
                .filter(this::estValide)
                .map(planification -> {
                    PlanificationHebdomadaire copie = new PlanificationHebdomadaire();
                    copie.setJour(planification.getJour());
                    copie.setHoraireDebut(planification.getHoraireDebut());
                    copie.setDuree(planification.getDuree());
                    copie.setDescriptif(normaliserDescriptif(planification.getDescriptif()));
                    copie.setSalle(trouverSalle(planification.getSalle()));
                    return copie;
                }).toList();
        long creneauxDistincts = planificationsNormalisees.stream()
                .map(planification -> planification.getJour() + "-" + planification.getHoraireDebut())
                .distinct()
                .count();
        if (creneauxDistincts != planificationsNormalisees.size()) {
            throw new IllegalArgumentException("Deux séances hebdomadaires ne peuvent pas avoir le même jour et le même horaire");
        }
        return planificationsNormalisees;
    }

    private boolean estValide(PlanificationHebdomadaire planification) {
        return planification.getJour() != null && planification.getHoraireDebut() != null
                && planification.getDuree() != null && planification.getDuree() > 0;
    }

    private String normaliserDescriptif(String descriptif) {
        if (descriptif == null || descriptif.isBlank()) {
            return null;
        }
        String valeur = descriptif.trim();
        if (valeur.length() > 100) {
            throw new IllegalArgumentException("Le descriptif d'une séance ne peut pas dépasser 100 caractères");
        }
        return valeur;
    }

    private void remplacerPlanifications(Activite activite, List<PlanificationHebdomadaire> planifications) {
        activite.getPlanificationsHebdomadaires().clear();
        for (PlanificationHebdomadaire planification : planifications) {
            planification.setActivite(activite);
            activite.getPlanificationsHebdomadaires().add(planification);
        }
    }

    private void synchroniserChampsHistoriques(Activite activite, List<PlanificationHebdomadaire> planifications) {
        if (planifications.isEmpty()) {
            return;
        }
        PlanificationHebdomadaire premierePlanification = planifications.getFirst();
        activite.setJour(premierePlanification.getJour());
        activite.setHoraireDebut(premierePlanification.getHoraireDebut());
        activite.setDuree(premierePlanification.getDuree());
        activite.setSalle(premierePlanification.getSalle());
    }

    private Salle trouverSalle(Salle salle) {
        if (salle == null || salle.getId() == null) {
            return null;
        }
        return salleRepository.findById(salle.getId())
                .orElseThrow(() -> new IllegalArgumentException("La salle sélectionnée n'existe plus"));
    }

    private Set<Adherent> resoudreReferentsValides(Activite activite, Set<Adherent> referents) {
        Set<Long> adherentsValides = activite.getAdhesions().stream()
                .filter(adhesion -> Status.VALIDEE.label.equals(adhesion.getStatutActuel()))
                .map(adhesion -> adhesion.getAdherent().getId())
                .collect(Collectors.toSet());
        Set<Adherent> referentsResolus = referents.stream()
                .map(referent -> adherentServices.getById(referent.getId()))
                .collect(Collectors.toSet());
        if (!referentsResolus.stream().allMatch(referent -> adherentsValides.contains(referent.getId()))) {
            throw new IllegalArgumentException("Un référent doit avoir une adhésion validée à cette activité");
        }
        return referentsResolus;
    }

    private void ajouterRoleReferent(Adherent adherent) {
        if (adherent.getUser() != null && adherent.getUser().getRoles().stream()
                .noneMatch(role -> role.getName() == ERole.ROLE_REFERENT)) {
            Role roleReferent = roleRepository.findByName(ERole.ROLE_REFERENT).orElseThrow();
            adherent.getUser().getRoles().add(roleReferent);
        }
    }

    private void retirerRoleReferent(Adherent adherent) {
        if (adherent.getUser() != null) {
            adherent.getUser().getRoles().removeIf(role -> role.getName() == ERole.ROLE_REFERENT);
        }
    }

    public List<com.wild.corp.adhesion.models.resources.AdherentLite> getReferentsCandidates(Long activiteId) {
        Activite activite = getById(activiteId);
        return adherentServices.getLites(activite.getAdhesions().stream()
                .filter(adhesion -> Status.VALIDEE.label.equals(adhesion.getStatutActuel()))
                .map(Adhesion::getAdherent)
                .collect(Collectors.toSet()));
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

}
