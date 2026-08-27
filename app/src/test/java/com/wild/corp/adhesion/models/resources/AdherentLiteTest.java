package com.wild.corp.adhesion.models.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdherentLiteTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ignoresTheNestedAdherentWhenReadingTheUserSentByTheForm() throws Exception {
        AdherentLite adherent = objectMapper.readValue("""
                {
                  "user": {
                    "username": "nouvel.adherent@example.org",
                    "adherent": { "id": 12 }
                  }
                }
                """, AdherentLite.class);

        assertThat(adherent.getUser().getUsername()).isEqualTo("nouvel.adherent@example.org");
    }

    @Test
    void ignoresAdhesionsWhenReadingAnAdherentUpdate() throws Exception {
        AdherentLite adherent = objectMapper.readValue("""
                {
                  "id": 12,
                  "adhesions": [{
                    "activite": { "salle": { "id": 1, "nom": "Gymnase" } }
                  }]
                }
                """, AdherentLite.class);

        assertThat(adherent.getId()).isEqualTo(12L);
        assertThat(adherent.getAdhesions()).isNull();
    }
}
