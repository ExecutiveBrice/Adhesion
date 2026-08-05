package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.repository.ActiviteNm1Repository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class ActiviteServices {

    @Autowired
    ActiviteRepository activiteRepository;

    @Autowired
    AdherentServices adherentServices;
    @Autowired
    ActiviteNm1Repository activiteNm1Repository;
    @Autowired
    SeanceServices seanceServices;

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
        activites.stream().forEach(activite -> {
            activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(Adhesion::isValide).count());
            activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(Adhesion::isEnCours).count());
            activite.setNbAdhesionsAttente(activite.getAdhesions().stream().filter(Adhesion::isEnAttente).count());
        });
        return activites;
    }

    public Activite save(Activite activite) {
        if (activite.getId() != null) {
            Activite activiteInDB = activiteRepository.findById(activite.getId()).orElseThrow();
            activiteInDB.getProfs().forEach(adherent -> adherent.getCours().remove(activiteInDB));

            BeanUtils.copyProperties(activite, activiteInDB,
                    "id", "adhesions", "sousClassement", "profs", "seances",
                    "nbAdhesionsEnCours", "nbAdhesionsCompletes", "nbAdhesionsAttente", "montantCollecte");

            activiteInDB.setProfs(activite.getProfs().stream()
                    .map(adherent -> adherentServices.getById(adherent.getId()))
                    .collect(Collectors.toSet()));
            activiteInDB.getProfs().forEach(adherent -> adherent.getCours().add(activiteInDB));

            return activiteRepository.save(activiteInDB);
        }

        seanceServices.fillSeances(activite, 29);
        return activiteRepository.save(activite);
    }

    public Activite addReferent(Long activiteId, Long adherentId) {
        Activite activiteDB = activiteRepository.findById(activiteId).get();
        Adherent adherent = adherentServices.getById(adherentId);
        activiteDB.getProfs().add(adherent);

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
