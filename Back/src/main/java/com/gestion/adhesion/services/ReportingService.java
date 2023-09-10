package com.gestion.adhesion.services;

import com.gestion.adhesion.models.Activite;
import com.gestion.adhesion.models.Paiement;
import com.gestion.adhesion.models.ReportingActivite;
import com.gestion.adhesion.models.ReportingAdhesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.gestion.adhesion.utils.Constantes.*;
import static java.util.stream.Collectors.groupingBy;

@Service
public class ReportingService {

    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    AdhesionServices adhesionServices;




    public List<ReportingActivite> getAllActiviteBasket(){
        Map<String, List<Activite>> activieFiltre = activiteServices.getAll().stream().filter(activite -> activite.getGroupe().equals("ALOD_B"))
                .collect(groupingBy(Activite::getGroupeFiltre));

        List<ReportingActivite> listRA = new ArrayList<>();
              activieFiltre.forEach((integers, activites) -> {
                  ReportingActivite ra = new ReportingActivite();
                  ra.setNomActivite(integers);

                  activites.forEach(activite -> {
                      ra.setNbF(ra.getNbF() + getByValidByGenre(activite, "Féminin"));
                      ra.setNbM(ra.getNbM() + getByValidByGenre(activite, "Masculin"));
                      ra.setNbInitee(ra.getNbInitee() + getByInitieeNonPayee(activite));
                      ra.setNbPayee(ra.getNbPayee() + getByInitieePayee(activite));
                      ra.setNbValidee(ra.getNbValidee() + getByValid(activite));
                      ra.setCotisations(ra.getCotisations() + getCotisationByStatut(activite));
                  });
                  listRA.add(ra);
              });

        return listRA;
    }

    public List<ReportingAdhesion> getAllAdhesions(LocalDate debut, LocalDate fin) {

        int nbJours = fin.plusDays(1).atStartOfDay().getDayOfYear() - debut.atStartOfDay().getDayOfYear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
        List<ReportingAdhesion> reporting = new ArrayList<>();

        for (int i = 0; i < nbJours; i++) {
            ReportingAdhesion reportingAdhesion = new ReportingAdhesion();
            reportingAdhesion.setX(debut.plusDays(i).format(formatter));
            reportingAdhesion.setNbInitiee(getInitnbByDate(debut.plusDays(i)));
            reportingAdhesion.setNbPayee(getPayenbByDate(debut.plusDays(i)));
            reportingAdhesion.setNbValidee(getVnbByDate(debut.plusDays(i)));

            reporting.add(reportingAdhesion);
        }
        return reporting;
    }

    public Long getInitnbByDate(LocalDate jourJ){
     return adhesionServices.getAll().stream().filter(adh -> STATUTS_ENCOURS.contains(adh.getStatutActuel())  && !adh.getValidPaiementSecretariat() && adh.getDateAjoutPanier().isBefore(jourJ.plusDays(1))).count();
    }

    public Long getPayenbByDate(LocalDate jourJ){
        return adhesionServices.getAll().stream().filter(adh -> STATUTS_ENCOURS.contains(adh.getStatutActuel())  && adh.getValidPaiementSecretariat() && adh.getDateAjoutPanier().isBefore(jourJ.plusDays(1))).count();
    }


    public Long getVnbByDate(LocalDate jourJ){
        return adhesionServices.getAll().stream().filter(adh -> STATUTS_VALIDES.contains(adh.getStatutActuel()) && adh.getDateChangementStatut().isBefore(jourJ.plusDays(1))).count();
    }


    public List<ReportingActivite> getAllActiviteGeneral(){
        Map<String, List<Activite>> activieFiltre = activiteServices.getAll().stream().filter(activite -> activite.getGroupe().equals("ALOD_G"))
                .collect(groupingBy(Activite::getGroupeFiltre));

        List<ReportingActivite> listRA = new ArrayList<>();
        activieFiltre.forEach((integers, activites) -> {
            ReportingActivite ra = new ReportingActivite();
            ra.setNomActivite(integers);

            activites.forEach(activite -> {
                ra.setNbF(ra.getNbF() + getByValidByGenre(activite, "Féminin"));
                ra.setNbM(ra.getNbM() + getByValidByGenre(activite, "Masculin"));
                ra.setNbInitee(ra.getNbInitee() + getByInitieeNonPayee(activite));
                ra.setNbPayee(ra.getNbPayee() + getByInitieePayee(activite));
                ra.setNbValidee(ra.getNbValidee() + getByValid(activite));
                ra.setCotisations(ra.getCotisations() + getCotisationByStatut(activite));
            });
            listRA.add(ra);
        });
        return listRA;
    }

    private Long getByValidByGenre(Activite activite, String genre){
        return activite.getAdhesions().stream().filter(adh -> STATUTS_VALIDES.contains(adh.getStatutActuel()) && adh.getAdherent().getGenre().equals(genre)).count();
    }

    private Long getByValid(Activite activite){
        return activite.getAdhesions().stream().filter(adh -> STATUTS_VALIDES.contains(adh.getStatutActuel())).count();
    }

    private Long getByInitieePayee(Activite activite){
        return activite.getAdhesions().stream().filter(adh -> STATUTS_ENCOURS.contains(adh.getStatutActuel()) && adh.getValidPaiementSecretariat()).count();
    }

    private Long getByInitieeNonPayee(Activite activite){
        return activite.getAdhesions().stream().filter(adh -> STATUTS_ENCOURS.contains(adh.getStatutActuel()) && !adh.getValidPaiementSecretariat()).count();
    }



    private Long getCotisationByStatut(Activite activite){
        activite.setMontantCollecte(0L);
        activite.getAdhesions()
                .forEach(adh -> activite.setMontantCollecte(activite.getMontantCollecte()+
                                adh.getPaiements().stream()
                                .map(Paiement::getMontant)
                                .reduce(0, (t, t2) -> t + t2)
                        ));
        return activite.getMontantCollecte();
    }
}
