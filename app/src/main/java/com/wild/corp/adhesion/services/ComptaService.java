package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.ComptaActivite;
import com.wild.corp.adhesion.models.Paiement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
public class ComptaService {

    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    AdhesionServices adhesionServices;

    public List<ComptaActivite> getAll(LocalDate dateDebut, LocalDate dateFin){

        Map<String, List<Activite>> activiteList = activiteServices.getAll().stream().collect(groupingBy(Activite::getNom));;

        List<ComptaActivite> listCA = new ArrayList<>();

        activiteList.forEach((nomActivite, activite) -> {
            ComptaActivite ca = new ComptaActivite();

            ca.setNomActivite(nomActivite);
            ca.setHelloAsso(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "HelloAsso"));
            ca.setHelloAsso3x(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "HelloAsso 3X"));
            ca.setCheque(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "Chèque"));
            ca.setCheque3x(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "Chèque 3X"));
            ca.setEspece(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "Pass sport"));
            ca.setPassport(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "Espèces"));
            ca.setAutre(totalTypePaiementSurPeriodeParActivite(dateDebut, dateFin, nomActivite, "Autre"));
            listCA.add(ca);
        });
        return listCA;
    }


    public Integer totalTypePaiementSurPeriodeParActivite(LocalDate dateDebut, LocalDate dateFin, String nomActivite, String typePaiement){

          return adhesionServices.getAll().stream().flatMap(adhesion -> {
                      return adhesion.getPaiements().stream().filter(paiement ->
                      typePaiement.equals(paiement.getTypeReglement()) && adhesion.getValidPaiementSecretariat() && paiement.getDateReglement() != null && paiement.getDateReglement().isBefore(dateFin.plusDays(1L)) && paiement.getDateReglement().isAfter(dateDebut.minusDays(1L)) && adhesion.getActivite().getNom().equals(nomActivite)
                      );
                  })
                  .map(Paiement::getMontant)
                  .reduce(0, (t, t2) -> t + t2);
        }
    }
