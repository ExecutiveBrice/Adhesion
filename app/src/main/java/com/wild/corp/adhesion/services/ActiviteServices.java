package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Seance;
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
    UserServices userServices;
    @Autowired
    SeanceServices seanceServices;

    public List<Seance> getSeancesDuJour(Long activiteId) {

        Activite activite = getById(activiteId);
        LocalDate now = LocalDate.now();
        return activite.getSeances().stream().filter(seance -> now.equals(seance.getDateSeance())).toList();
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

    public Activite getById(Long activiteId) {
        return activiteRepository.findById(activiteId).get();
    }

}
