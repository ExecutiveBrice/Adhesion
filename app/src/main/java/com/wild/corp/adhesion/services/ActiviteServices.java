package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.wild.corp.adhesion.utils.Constantes.*;

@Service
public class ActiviteServices {

    @Autowired
    ActiviteRepository activiteRepository;

    @Autowired
    AdherentServices adherentServices;

    public List<Activite> getAll (){
        List<Activite> activites = activiteRepository.findAll();
        activites.stream().forEach(activite -> {
            activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(adh -> STATUTS_VALIDES.contains(adh.getStatutActuel())).count());
            activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(adh -> STATUTS_ENCOURS.contains(adh.getStatutActuel())).count());
            activite.setNbAdhesionsAttente(activite.getAdhesions().stream().filter(adh -> STATUTS_ENATTENTE.contains(adh.getStatutActuel())).count());
        });
        return activites;
    }

    public Activite save(Activite activite){
        Optional<Activite> activiteInDB = activiteRepository.findById(activite.getId());
        activiteInDB.ifPresent(value -> value.getProfs().forEach(adherent -> adherent.getCours().remove(value)));

        activite.setProfs(activite.getProfs().stream().map(adherent -> adherentServices.getBasicById(adherent.getId())).collect(Collectors.toSet()));

        activite.getProfs().forEach(adherent -> adherent.getCours().add(activite));

        return activiteRepository.save(activite);
    }

    public Activite addReferent(Long activiteId, Long adherentId){
        Activite activiteDB = activiteRepository.findById(activiteId).get();
        Adherent adherent = adherentServices.getById(adherentId);
        activiteDB.getProfs().add(adherent);

        return activiteRepository.save(activiteDB);
    }

    public List<Activite> findByNom(String nom){
        return activiteRepository.findByNom(nom);
    }

    public Activite getById(Long activiteId){
        return activiteRepository.findById(activiteId).get();
    }

    public boolean existsByNom(String nom){
        return activiteRepository.existsByNom(nom);
    }

}
