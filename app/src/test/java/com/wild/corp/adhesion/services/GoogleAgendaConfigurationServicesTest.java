package com.wild.corp.adhesion.services;

import tools.jackson.databind.json.JsonMapper;
import com.wild.corp.adhesion.models.GoogleAgenda;
import com.wild.corp.adhesion.models.resources.AgendaGoogleConfiguration;
import com.wild.corp.adhesion.repository.GoogleAgendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAgendaConfigurationServicesTest {

    private GoogleAgendaRepository googleAgendaRepository;
    private ParamTextServices paramTextServices;
    private GoogleAgendaConfigurationServices service;

    @BeforeEach
    void setUp() {
        googleAgendaRepository = mock(GoogleAgendaRepository.class);
        paramTextServices = mock(ParamTextServices.class);
        service = new GoogleAgendaConfigurationServices(
                googleAgendaRepository,
                paramTextServices,
                new GoogleAgendaServices(HttpClient.newHttpClient()),
                new JsonMapper());
    }

    @Test
    void createsANamedAgendaWithNormalizedSourceAndColor() {
        when(googleAgendaRepository.save(any())).thenAnswer(invocation -> {
            GoogleAgenda agenda = invocation.getArgument(0);
            agenda.setId(12L);
            return agenda;
        });

        AgendaGoogleConfiguration agenda = service.create(new AgendaGoogleConfiguration(
                null,
                "Événements ALOD",
                "https://calendar.google.com/calendar/embed?src=demo%40group.calendar.google.com",
                "#a1b2c3"));

        assertThat(agenda).isEqualTo(new AgendaGoogleConfiguration(
                12L, "Événements ALOD", "demo@group.calendar.google.com", "#A1B2C3"));
    }

    @Test
    void updatesTheNameSourceAndColorOfAnExistingAgenda() {
        GoogleAgenda agenda = GoogleAgenda.builder()
                .id(8L)
                .nom("Ancien nom")
                .source("ancien@group.calendar.google.com")
                .couleur("#4285F4")
                .build();
        when(googleAgendaRepository.findById(8L)).thenReturn(Optional.of(agenda));
        when(googleAgendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgendaGoogleConfiguration resultat = service.update(8L, new AgendaGoogleConfiguration(
                8L, "Nouvel agenda", "nouveau@group.calendar.google.com", "#DB4437"));

        assertThat(resultat.nom()).isEqualTo("Nouvel agenda");
        assertThat(resultat.source()).isEqualTo("nouveau@group.calendar.google.com");
        assertThat(resultat.couleur()).isEqualTo("#DB4437");
    }

    @Test
    void migratesTheLegacyTextConfigurationIntoTheDedicatedTable() {
        when(paramTextServices.getParamValueOrDefault("Google_Agendas", ""))
                .thenReturn("premier@group.calendar.google.com\nsecond@group.calendar.google.com");
        when(googleAgendaRepository.findAllByOrderByNomAsc()).thenReturn(List.of());

        service.getAll();

        verify(googleAgendaRepository).saveAll(any());
    }

    @Test
    void rejectsMissingNamesAndInvalidColors() {
        assertThatThrownBy(() -> service.create(new AgendaGoogleConfiguration(
                null, "", "demo@group.calendar.google.com", "blue")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nom");
    }
}
