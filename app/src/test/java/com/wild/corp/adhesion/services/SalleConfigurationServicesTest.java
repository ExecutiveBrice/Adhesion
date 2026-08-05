package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Salle;
import com.wild.corp.adhesion.models.resources.SalleConfiguration;
import com.wild.corp.adhesion.repository.SalleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SalleConfigurationServicesTest {

    private SalleRepository salleRepository;
    private SalleConfigurationServices service;

    @BeforeEach
    void setUp() {
        salleRepository = mock(SalleRepository.class);
        service = new SalleConfigurationServices(salleRepository);
    }

    @Test
    void createsASalleWithNormalizedValues() {
        when(salleRepository.save(any())).thenAnswer(invocation -> {
            Salle salle = invocation.getArgument(0);
            salle.setId(12L);
            return salle;
        });

        SalleConfiguration salle = service.create(new SalleConfiguration(
                null, "  Salle polyvalente ", " 12 rue des Fleurs ", "#a1b2c3"));

        assertThat(salle).isEqualTo(new SalleConfiguration(
                12L, "Salle polyvalente", "12 rue des Fleurs", "#A1B2C3"));
    }

    @Test
    void rejectsAMissingAddress() {
        assertThatThrownBy(() -> service.create(new SalleConfiguration(null, "Salle", "", "#4285F4")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("adresse");
    }
}
