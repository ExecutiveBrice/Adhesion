package com.wild.corp.adhesion.models.resources;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Seance;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SeanceResponseTest {

    @Test
    void activitySessionResponseDoesNotRepeatTheActivityGraph() throws Exception {
        Activite activite = new Activite();
        activite.setId(38L);
        activite.setNom("Loisir F");
        Seance seance = new Seance();
        seance.setId(3626L);
        seance.setActivite(activite);
        seance.setDebut(LocalDateTime.of(2026, 11, 24, 20, 45));

        SeanceResponse response = SeanceResponse.from(seance);
        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(response);

        assertThat(response.id()).isEqualTo(3626L);
        assertThat(response.activite()).isNull();
        assertThat(json).contains("\"id\":3626").doesNotContain("Loisir F", "planificationsHebdomadaires");
    }
}
