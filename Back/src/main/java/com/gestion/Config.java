package com.gestion;


import com.gestion.adhesion.models.Adhesion;
import com.gestion.adhesion.services.AdhesionServices;
import com.gestion.adhesion.services.EmailService;
import com.gestion.adhesion.services.ParamBooleanServices;
import com.gestion.adhesion.services.ParamNumberServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@EnableScheduling
public class Config {

    @Autowired
    AdhesionServices adhesionServices;
    @Autowired
    ParamBooleanServices paramBooleanServices;
    @Autowired
    ParamNumberServices paramNumberServices;
    @Autowired
    EmailService emailService;
    @Scheduled(cron = "* * 1 * * ?", zone = "Europe/Paris")
    public void tachesJournalieres() {

        if(paramBooleanServices.findByParamValue("Mail_Annulation")) {
            annulation();
        }
        if(paramBooleanServices.findByParamValue("Mail_Rappel")) {
            rappel();
        }
    }


    private void rappel(){

        LocalDate dt2 = LocalDate.now();
        dt2.minus(paramNumberServices.findByParamValue("Jours_Avant_Rappel"), ChronoUnit.DAYS);

        adhesionServices.getAll().stream().filter(adhesion ->
                ("Attente validation adhérent".equals(adhesion.getStatutActuel()) || "Attente validation secrétariat".equals(adhesion.getStatutActuel())) &&
                        (adhesion.getAccords().stream().anyMatch(accord -> accord.getDatePassage() == null) || !adhesion.getValidPaiementSecretariat()) &&
                        adhesion.getDateChangementStatut()==null?adhesion.getDateAjoutPanier().atStartOfDay().isBefore(dt2.atStartOfDay()):adhesion.getDateChangementStatut().atStartOfDay().isBefore(dt2.atStartOfDay()) &&
                        (adhesion.getRappel()==null || !adhesion.getRappel())).forEach(adhesion -> {
            System.out.println("rappel "+adhesion.getId());
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Rappel", "Corp_Mail_Rappel");
            adhesion.setRappel(true);
            adhesionServices.saveUnique(adhesion);
        });
    }


    private void annulation(){

        LocalDate dt2 = LocalDate.now();
        dt2.minus(paramNumberServices.findByParamValue("Jours_Avant_Annulation"), ChronoUnit.DAYS);

        adhesionServices.getAll().stream().filter(adhesion ->
                ("Attente validation adhérent".equals(adhesion.getStatutActuel()) || "Attente validation secrétariat".equals(adhesion.getStatutActuel())) &&
                        (adhesion.getAccords().stream().anyMatch(accord -> accord.getDatePassage() == null) || !adhesion.getValidPaiementSecretariat()) &&
                        adhesion.getDateChangementStatut()==null?adhesion.getDateAjoutPanier().atStartOfDay().isBefore(dt2.atStartOfDay()):adhesion.getDateChangementStatut().atStartOfDay().isBefore(dt2.atStartOfDay()) &&
                        adhesion.getRappel()).forEach(adhesion -> {
            System.out.println("annulation "+adhesion.getId());
              adhesionServices.choisirStatut(adhesion.getId(),"Annulée");


            List<Adhesion> adhesions = adhesion.getActivite().getAdhesions().stream().map(adh -> ("Attente validation adhérent".equals(adh.getStatutActuel()) || "Attente validation secrétariat".equals(adh.getStatutActuel()) || "Validée".equals(adh.getStatutActuel()))?adh:null).toList();
            List<Adhesion> adhesionsAttente = adhesion.getActivite().getAdhesions().stream().filter(adh -> "Sur liste d'attente".equals(adh.getStatutActuel())).toList();

            if(adhesions.size() < adhesion.getActivite().getNbPlaces()){
                Adhesion lastAdhesion = null;
                for (Adhesion adh : adhesionsAttente){
                    if(lastAdhesion != null && adh.getDateAjoutPanier().isAfter(lastAdhesion.getDateAjoutPanier())){
                        lastAdhesion = adh;
                    }else{
                        lastAdhesion = adh;
                    }
                }

                if(lastAdhesion != null){
                    adhesionServices.choisirStatut(lastAdhesion.getId(),"Attente validation adhérent");
                }
            }

        });
    }
}
