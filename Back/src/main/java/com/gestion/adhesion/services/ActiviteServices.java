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

    public static final List<String> list_G_valid = List.of("Validée", "Validée, en attente du certificat médical");
    public static final List<String> list_G_encours = List.of("Attente validation adhérent", "Attente validation secrétariat");

    public static final List<String> list_B_valid = List.of("Validée", "Validée, en attente du certificat médical", "Licence T", "Retour Comité", "Licence générée", "Validée groupement sportif");
    public static final List<String> list_B_encours = List.of("Attente validation adhérent", "Attente validation secrétariat");
    public List<Activite> getAll (){
        List<Activite> activites = activiteRepository.findAll();
        activites.stream().forEach(activite -> {
            if("ALOD_G".equals(activite.getGroupe())){
                activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(adh -> list_G_valid.contains(adh.getStatutActuel())).count());
                activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(adh -> list_G_encours.contains(adh.getStatutActuel())).count());
                activite.setMontantCollecte(0L);
                activite.getAdhesions().stream().filter(adh -> list_B_valid.contains(adh.getStatutActuel()))
                        .forEach(adh -> activite.setMontantCollecte(activite.getMontantCollecte()+adh.getTarif()));
            }
            if("ALOD_B".equals(activite.getGroupe())){
                activite.setNbAdhesionsCompletes(activite.getAdhesions().stream().filter(adh -> list_B_valid.contains(adh.getStatutActuel())).count());
                activite.setNbAdhesionsEnCours(activite.getAdhesions().stream().filter(adh -> list_B_encours.contains(adh.getStatutActuel())).count());
                activite.setMontantCollecte(0L);
                activite.getAdhesions().stream().filter(adh -> list_B_valid.contains(adh.getStatutActuel()))
                        .forEach(adh -> activite.setMontantCollecte(activite.getMontantCollecte()+adh.getTarif()));
            }
        });
        return activites;
    }

    public Activite save(Activite activite){

        activite.setProfs(activite.getProfs().stream().map(adherent -> adherentServices.getById(adherent.getId())).collect(Collectors.toSet()));

        activite.getProfs().stream().forEach(adherent -> adherent.getCours().add(activite));

        return activiteRepository.save(activite);
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
