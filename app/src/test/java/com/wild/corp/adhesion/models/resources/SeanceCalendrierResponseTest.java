package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.PlanificationHebdomadaire;
import com.wild.corp.adhesion.models.Seance;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SeanceCalendrierResponseTest {
    @Test
    void exposesActivityNameAndSessionDescriptionSeparatelyWhilePreservingCalendarTitle() {
        Activite activite = new Activite();
        activite.setNom("Basket");
        Seance seance = new Seance();
        seance.setActivite(activite);
        seance.setDebut(LocalDateTime.of(2026, 10, 6, 18, 0));
        seance.setDescriptif("  Équipe U15  ");
        seance.setCauseAnnulation("Gymnase indisponible");

        var response = SeanceCalendrierResponse.from(seance);
        assertThat(response.activiteNomCourt()).isEqualTo("Basket");
        assertThat(response.descriptif()).isEqualTo("Équipe U15");
        assertThat(response.activiteNom()).isEqualTo("Basket – Équipe U15");
        assertThat(response.causeAnnulation()).isEqualTo("Gymnase indisponible");

        PlanificationHebdomadaire planification = new PlanificationHebdomadaire();
        planification.setJour(seance.getDebut().getDayOfWeek());
        planification.setHoraireDebut(seance.getDebut().toLocalTime());
        planification.setDescriptif("  Entraînement U15  ");
        activite.getPlanificationsHebdomadaires().add(planification);
        response = SeanceCalendrierResponse.from(seance);
        assertThat(response.descriptif()).isEqualTo("Entraînement U15");
        assertThat(response.activiteNomCourt()).isEqualTo("Basket");
        assertThat(response.activiteNom()).isEqualTo("Basket – Entraînement U15");
    }
}
