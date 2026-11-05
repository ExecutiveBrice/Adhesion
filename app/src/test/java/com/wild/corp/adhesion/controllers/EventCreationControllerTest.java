package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.EventCreationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventCreationController.class)
@ContextConfiguration(classes = {EventCreationController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class EventCreationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EventCreationService service;
    private static final String REQUEST = """
        {"evenement":{"equipes":[2,3],"type":"ACTIVITE","debut":"2027-03-20T18:00","fin":"2027-03-20T19:00"},
        "sections":[7,8],"semaines":6}
        """;
    @Test void acceptsMultipleSectionsTeamsAndAWeeklyPeriod() throws Exception {
        when(service.create(any(), eq("admin"))).thenReturn(new EventCreationService.Created(4, List.of()));
        mvc.perform(post("/teams/events").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value(4));
        verify(service).create(argThat(r -> r.semaines() == 6 && r.sections().equals(List.of(7L, 8L))
                && r.evenement().equipes().equals(List.of(2L, 3L))), eq("admin"));
    }
    @Test void returnsTheCalendarFailureAsAnActionableError() throws Exception {
        when(service.create(any(), anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Calendrier indisponible"));
        mvc.perform(post("/teams/events").with(user("admin")).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value("Calendrier indisponible"));
    }
    @Test void exposesOnlyTheOptionsReturnedForTheAuthenticatedUser() throws Exception {
        when(service.options("coach")).thenReturn(new EventCreationService.Options(List.of(), List.of(new EventCreationService.SectionTarget(7L, "Danse"))));
        mvc.perform(get("/teams/event-options").with(user("coach")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sections[0].nom").value("Danse"));
        verify(service).options("coach");
    }
}
