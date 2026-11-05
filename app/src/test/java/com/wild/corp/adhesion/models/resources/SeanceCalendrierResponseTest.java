package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Equipe;
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

        Equipe team = new Equipe(); team.setId(2L); team.setNom("U15 A"); team.setActivite(activite);
        seance.getEquipes().add(team);
        response = SeanceCalendrierResponse.from(seance);
        assertThat(response.descriptif()).isEqualTo("U15 A");
        assertThat(response.activiteNomCourt()).isEqualTo("Basket");
        assertThat(response.activiteNom()).isEqualTo("Basket – U15 A");
    }
}
