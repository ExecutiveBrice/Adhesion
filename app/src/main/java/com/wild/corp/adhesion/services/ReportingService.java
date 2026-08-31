package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingService {

    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    AdhesionServices adhesionServices;




    public List<ReportingActivite> getAllActiviteBasket(){
        return getReportingActivites("ALOD_B");
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
     return adhesionServices.getAll().stream().filter(adh -> adh.getDateAjoutPanier().isBefore(jourJ.plusDays(1))).count();
    }

    public Long getPayenbByDate(LocalDate jourJ){
        return adhesionServices.getAll().stream().filter(adh -> adh.getValidPaiementSecretariat() && adh.getDateAjoutPanier().isBefore(jourJ.plusDays(1))).count();
    }

    public Long getVnbByDate(LocalDate jourJ){
        return adhesionServices.getAll().stream().filter(adh -> adh.isValide() && adh.getDateChangementStatut().isBefore(jourJ.plusDays(1))).count();
    }

    public List<ReportingActivite> getAllActiviteGeneral(){
        return getReportingActivites("ALOD_G");
    }

    private List<ReportingActivite> getReportingActivites(String groupe) {
        List<ReportingActivite> listRA = new ArrayList<>();
        activiteServices.getAll().stream()
                .filter(activite -> groupe.equals(activite.getGroupe()))
                .forEach(activite -> {
            ReportingActivite ra = new ReportingActivite();
            ra.setNomActivite(activite.getNom());
            ra.setGroupe(activite.getGroupeFiltre());
            ra.setNbF(getByValidByGenre(activite, "Féminin"));
            ra.setNbM(getByValidByGenre(activite, "Masculin"));
            ra.setNbInitee(getByInitieeNonPayee(activite));
            ra.setNbPayee(getByInitieePayee(activite));
            ra.setNbValidee(getByValid(activite));
            ra.setCotisations(getCotisationByStatut(activite));
            listRA.add(ra);
        });
        return listRA;
    }

    private Long getByValidByGenre(Activite activite, String genre){
        return activite.getAdhesions().stream().filter(adh -> adh.isValide() && adh.getAdherent().getGenre().equals(genre)).count();
    }

    private Long getByValid(Activite activite){
        return activite.getAdhesions().stream().filter(Adhesion::isValide).count();
    }

    private Long getByInitieePayee(Activite activite){
        return activite.getAdhesions().stream().filter(adh -> adh.isEnCours() && adh.getValidPaiementSecretariat()).count();
    }

    private Long getByInitieeNonPayee(Activite activite){
        return activite.getAdhesions().stream().filter(adh -> adh.isEnCours() && !adh.getValidPaiementSecretariat()).count();
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
