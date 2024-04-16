package com.wild.corp.adhesion.config;


import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.EmailService;
import com.wild.corp.adhesion.services.ParamBooleanServices;
import com.wild.corp.adhesion.services.ParamNumberServices;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
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
        log.info("tachesJournalieres");
        if(paramBooleanServices.findByParamValue("Mail_Annulation")) {
            log.info("annulation");
            annulation();
        }
        if(paramBooleanServices.findByParamValue("Mail_Rappel")) {
            log.info("rappel");
            rappel();
        }
    }


    @Transactional
    private void rappel(){

        LocalDate dtr = LocalDate.now().minus(paramNumberServices.findByParamValue("Jours_Avant_Rappel"), ChronoUnit.DAYS);

        adhesionServices.getAll().stream().filter(adhesion ->
                ( Status.ATTENTE_ADHERENT.label.equals(adhesion.getStatutActuel()) ||  Status.ATTENTE_SECRETARIAT.label.equals(adhesion.getStatutActuel())) &&
                       adhesion.getDateAjoutPanier().atStartOfDay().isBefore(dtr.atStartOfDay()) &&
                       !adhesion.getRappel()).forEach(adhesion -> {
            log.info("rappel "+adhesion.getAdherent().getNom() +" "+adhesion.getAdherent().getPrenom()+" pour l'activité "+adhesion.getActivite().getNom()+" "+adhesion.getActivite().getHoraire());

            emailService.sendAutoMail(adhesion, "Sujet_Mail_Rappel", "Corp_Mail_Rappel", false);
            adhesion.setRappel(true);
            adhesionServices.saveUnique(adhesion);
        });
    }


    private void annulation(){

        LocalDate dta = LocalDate.now().minus(paramNumberServices.findByParamValue("Jours_Avant_Annulation"), ChronoUnit.DAYS);
        System.out.println(dta);
        adhesionServices.getAll().stream().filter(adhesion ->
                ( Status.ATTENTE_ADHERENT.label.equals(adhesion.getStatutActuel()) ||  Status.ATTENTE_SECRETARIAT.label.equals(adhesion.getStatutActuel())) &&
                        adhesion.getDateAjoutPanier().atStartOfDay().isBefore(dta.atStartOfDay()) &&
                        adhesion.getRappel()
        ).forEach(adhesion -> {

            log.info("annulation "+adhesion.getAdherent().getNom() +" "+adhesion.getAdherent().getPrenom()+" pour l'activité "+adhesion.getActivite().getNom()+" "+adhesion.getActivite().getHoraire());


            adhesionServices.choisirStatut(adhesion.getId(), Status.ANNULEE.label);

            List<Adhesion> adhesions = adhesion.getActivite().getAdhesions().stream().map(adh -> ( Status.ATTENTE_ADHERENT.label.equals(adh.getStatutActuel()) ||  Status.ATTENTE_SECRETARIAT.label.equals(adh.getStatutActuel()) ||  Status.VALIDEE.label.equals(adh.getStatutActuel()))?adh:null).toList();
            List<Adhesion> adhesionsAttente = adhesion.getActivite().getAdhesions().stream().filter(adh ->  Status.LISTE_ATTENTE.label.equals(adh.getStatutActuel())).sorted((adh, t1) -> adh.getPosition()).toList();

            emailService.sendAutoMail(adhesion, "Sujet_Mail_Annulation_Automatique", "Corp_Mail_Annulation_Automatique", false);

            if(adhesions.size() < adhesion.getActivite().getNbPlaces() && adhesionsAttente.stream().findFirst().isPresent()){
                adhesionServices.choisirStatut(adhesionsAttente.stream().findFirst().get().getId(), Status.ATTENTE_ADHERENT.label);
            }

        });
    }
}
