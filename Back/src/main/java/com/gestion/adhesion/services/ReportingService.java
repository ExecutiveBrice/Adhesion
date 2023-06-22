package com.gestion.adhesion.services;

import com.gestion.adhesion.models.Activite;
import com.gestion.adhesion.models.ReportingActivite;
import com.gestion.adhesion.models.ReportingAdhesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
public class ReportingService {

    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    AdhesionServices adhesionServices;

    public static final List<String> list_G_valid = List.of("Validée", "Validée, en attente du certificat médical");
    public static final List<String> list_G_encours = List.of("Attente validation adhérent", "Attente validation secrétariat");

    public static final List<String> list_B_valid = List.of("Validée", "Validée, en attente du certificat médical", "Licence T", "Retour Comité", "Licence générée", "Validée groupement sportif");
    public static final List<String> list_B_encours = List.of("Attente validation adhérent", "Attente validation secrétariat");


    public List<ReportingActivite> getAllActiviteBasket(){
        Map<String, List<Activite>> activieFiltre = activiteServices.getAll().stream().filter(activite -> activite.getGroupe().equals("ALOD_B"))
                .collect(groupingBy(Activite::getGroupeFiltre));

        List<ReportingActivite> listRA = new ArrayList<>();
              activieFiltre.forEach((integers, activites) -> {
                  ReportingActivite ra = new ReportingActivite();
                  ra.setNomActivite(integers);

                  activites.forEach(activite -> {
                      ra.setNbF(ra.getNbF() + getByStatutGenre(activite, list_B_valid, "Féminin"));
                      ra.setNbM(ra.getNbM() + getByStatutGenre(activite, list_B_valid, "Masculin"));
                      ra.setNbEC(ra.getNbEC() + getByStatuts(activite, list_B_encours));
                      ra.setNbV(ra.getNbV() + getByStatuts(activite, list_B_valid));
                      ra.setCotisations(ra.getCotisations() + getCotisationByStatut(activite, list_B_valid));
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
            reportingAdhesion.setNbEC(getECnbByDate(debut.plusDays(i)));
            reportingAdhesion.setNbV(getVnbByDate(debut.plusDays(i)));

            reporting.add(reportingAdhesion);
        }
        return reporting;
    }

    public Long getECnbByDate(LocalDate jourJ){
     return adhesionServices.getAll().stream().filter(adh -> list_G_encours.contains(adh.getStatutActuel()) && adh.getDateAjoutPanier().isBefore(jourJ.plusDays(1))).count();
    }

    public Long getVnbByDate(LocalDate jourJ){
        return adhesionServices.getAll().stream().filter(adh -> list_G_valid.contains(adh.getStatutActuel()) && adh.getDateChangementStatut().isBefore(jourJ.plusDays(1))).count();
    }


    public List<ReportingActivite> getAllActiviteGeneral(){
        Map<String, List<Activite>> activieFiltre = activiteServices.getAll().stream().filter(activite -> activite.getGroupe().equals("ALOD_G"))
                .collect(groupingBy(Activite::getGroupeFiltre));

        List<ReportingActivite> listRA = new ArrayList<>();
        activieFiltre.forEach((integers, activites) -> {
            ReportingActivite ra = new ReportingActivite();
            ra.setNomActivite(integers);

            activites.forEach(activite -> {
                ra.setNbF(ra.getNbF() + getByStatutGenre(activite, list_G_valid, "Féminin"));
                ra.setNbM(ra.getNbM() + getByStatutGenre(activite, list_G_valid, "Masculin"));
                ra.setNbEC(ra.getNbEC() + getByStatuts(activite, list_G_encours));
                ra.setNbV(ra.getNbV() + getByStatuts(activite, list_G_valid));
                ra.setCotisations(ra.getCotisations() + getCotisationByStatut(activite, list_G_valid));
            });
            listRA.add(ra);
        });
        return listRA;
    }

    private Long getByStatutGenre(Activite activite, List<String> statuts_list, String genre){
        return activite.getAdhesions().stream().filter(adhesion -> statuts_list.contains(adhesion.getStatutActuel()) && adhesion.getAdherent().getGenre().equals(genre)).count();
    }

    private Long getByStatuts(Activite activite, List<String> statuts_list){
        return activite.getAdhesions().stream().filter(adh -> statuts_list.contains(adh.getStatutActuel())).count();
    }

    private Long getCotisationByStatut(Activite activite, List<String> statuts_list){
        activite.setMontantCollecte(0L);
        activite.getAdhesions().stream().filter(adh -> statuts_list.contains(adh.getStatutActuel()))
                .forEach(adh -> activite.setMontantCollecte(activite.getMontantCollecte()+adh.getTarif()));
        return activite.getMontantCollecte();
    }
}
