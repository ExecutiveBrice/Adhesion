package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
public class RappelServices {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final AdhesionServices adhesionServices;
    private final ParamNumberServices paramNumberServices;
    private final EmailService emailService;

    public RappelServices(AdhesionServices adhesionServices,
                          ParamNumberServices paramNumberServices,
                          EmailService emailService) {
        this.adhesionServices = adhesionServices;
        this.paramNumberServices = paramNumberServices;
        this.emailService = emailService;
    }

    @Transactional
    public int envoyerRappels() {
        LocalDate dateLimite = LocalDate.now(PARIS)
                .minusDays(paramNumberServices.findByParamValue("Jours_Avant_Rappel"));
        int rappelsEnvoyes = 0;

        for (Adhesion adhesion : adhesionServices.getAll()) {
            boolean enAttente = Status.ATTENTE_ADHERENT.label.equals(adhesion.getStatutActuel())
                    || Status.ATTENTE_SECRETARIAT.label.equals(adhesion.getStatutActuel());
            if (!enAttente || !adhesion.getDateAjoutPanier().isBefore(dateLimite) || adhesion.getRappel()) {
                continue;
            }

            log.info("Rappel pour {} {} pour l'activité {} {}",
                    adhesion.getAdherent().getNom(), adhesion.getAdherent().getPrenom(),
                    adhesion.getActivite().getNom(), adhesion.getActivite().getHoraire());
            emailService.sendAutoMail(adhesion, "Sujet_Mail_Rappel", "Corp_Mail_Rappel", false);
            adhesion.setRappel(true);
            adhesion.setRemarqueSecretariat((adhesion.getRemarqueSecretariat() != null
                    ? adhesion.getRemarqueSecretariat() : "") + " mail de rappel auto fait le " + LocalDate.now(PARIS));
            adhesionServices.saveUnique(adhesion);
            rappelsEnvoyes++;
        }

        return rappelsEnvoyes;
    }
}
