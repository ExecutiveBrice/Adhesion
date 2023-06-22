package com.gestion.adhesion.services;

import com.gestion.adhesion.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

          return adhesionServices.getAll().stream().filter(adhesion -> typePaiement.equals(adhesion.getTypeReglement()) && adhesion.getValidPaiementSecretariat() && adhesion.getDateReglement() != null && adhesion.getDateReglement().isBefore(dateFin) && adhesion.getDateReglement().isAfter(dateDebut) && adhesion.getActivite().getNom().equals(nomActivite))
                  .map(adhesion -> adhesion.getTarif())
                  .reduce(0, (t, t2) -> t + t2);
        }
    }
