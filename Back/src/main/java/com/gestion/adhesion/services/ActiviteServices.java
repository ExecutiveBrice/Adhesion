package com.gestion.adhesion.services;

import com.gestion.adhesion.models.Activite;
import com.gestion.adhesion.models.Adherent;
import com.gestion.adhesion.models.Paiement;
import com.gestion.adhesion.repository.ActiviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gestion.adhesion.utils.Constantes.*;

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

    public void fillActivites(){
        try (BufferedReader br = new BufferedReader(new FileReader("./listingActivites.csv"))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                Activite act = new Activite();
                act.setGroupe(values[0]);
                act.setNom(values[1]);
                act.setLien(values[2]);
                act.setSalle(values[3]);
                act.setTarif(Integer.parseInt(values[4]));
                act.setNbPlaces(Integer.parseInt(values[5]));
                act.setHoraire(values[6]);
                act.setPriseEnCharge(Boolean.parseBoolean(values[7]));
                act.setAutorisationParentale(Boolean.parseBoolean(values[8]));
                act.setAutorisationParentale(Boolean.parseBoolean(values[9]));
                act.setCertificatMedical(Boolean.parseBoolean(values[10]));
                act.setVieClub(Boolean.parseBoolean(values[11]));
                act.setPourEnfant(values[12]!=null?Boolean.parseBoolean(values[12]):null);
                save(act);

            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
