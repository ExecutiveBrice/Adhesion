package com.gestion.adhesion.services;

import com.gestion.adhesion.models.Activite;
import com.gestion.adhesion.repository.ActiviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActiviteServices {

    @Autowired
    ActiviteRepository activiteRepository;

    @Autowired
    AdherentServices adherentServices;

    public List<Activite> getAll (){
        List<Activite> activites = activiteRepository.findAll();
        activites.stream().forEach(activite -> {activite.setNbAdherentTotal(activite.getAdhesions().size());});
        return activites;
    }

    public Activite getById(Long id){
        return activiteRepository.findById(id).get();
    }

    public Activite save(Activite activite){

        activite.setProfs(activite.getProfs().stream().map(adherent -> adherentServices.getById(adherent.getId())).collect(Collectors.toSet()));

        activite.getProfs().stream().forEach(adherent -> adherent.getCours().add(activite));

        return activiteRepository.save(activite);
    }

    public Activite findByNom(String nom){
        return activiteRepository.findByNom(nom).get();
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
