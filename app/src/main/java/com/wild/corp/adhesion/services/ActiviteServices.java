package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.ActiviteNm1Repository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        return activite.getSeances().stream().filter(seance -> now.equals(seance.getDateSeance())).toList();
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
            Optional<Activite> activiteInDB = activiteRepository.findById(activite.getId());
            activiteInDB.ifPresent(value -> value.getProfs().forEach(adherent -> adherent.getCours().remove(value)));

            activite.setProfs(activite.getProfs().stream().map(adherent -> adherentServices.getById(adherent.getId())).collect(Collectors.toSet()));
            activite.getProfs().forEach(adherent -> adherent.getCours().add(activite));
            if(!activite.getJour().equals(activiteInDB.get().getJour())){
                seanceServices.modifyDay(activite);
            }
        }else{
           seanceServices.fillSeances(activite, 29);
        }

        return  activiteRepository.save(activite);
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
