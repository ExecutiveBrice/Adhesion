package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.resources.AdherentLite;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdherentServicesTest {

    private final AdherentServices adherentServices = new AdherentServices();

    @Test
    void updatesTheStateAndDateOfExistingAccords() {
        Accord accordEnBase = new Accord();
        accordEnBase.setId(41L);
        accordEnBase.setEtat(true);

        Adherent adherentEnBase = new Adherent();
        adherentEnBase.setAccords(List.of(accordEnBase));

        Accord accordFront = new Accord();
        accordFront.setId(41L);
        accordFront.setEtat(false);
        accordFront.setDatePassage(LocalDate.of(2026, 8, 25));
        AdherentLite adherentFront = new AdherentLite();
        adherentFront.setAccords(List.of(accordFront));

        adherentServices.updateAccords(adherentFront, adherentEnBase);

        assertThat(accordEnBase.getEtat()).isFalse();
        assertThat(accordEnBase.getDatePassage()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void ignoresAnIncompleteAccordWhenCheckingWhetherAnAdherentIsComplete() {
        Adherent adherent = new Adherent();
        adherent.setNom("MARTIN");
        adherent.setPrenom("Alice");
        adherent.setGenre("Féminin");
        adherent.setNaissance(LocalDate.of(2000, 1, 1));
        adherent.setLieuNaissance("44");
        adherent.setAdresse("1 rue des Fleurs");
        adherent.setTelephone("0600000000");

        Accord accordSansNom = new Accord();
        Accord rgpd = new Accord();
        rgpd.setNom("RGPD");
        rgpd.setEtat(true);
        Accord droitImage = new Accord();
        droitImage.setNom("DroitImage");
        droitImage.setEtat(true);
        adherent.setAccords(List.of(accordSansNom, rgpd, droitImage));

        adherentServices.isComplet(adherent);

        assertThat(adherent.getCompletAdhesion()).isTrue();
    }

    @Test
    void refusesToCreateAnAdherentWithoutATribu() {
        assertThatThrownBy(() -> adherentServices.saveNewAdherent(new AdherentLite()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tribu obligatoire");
    }
}
