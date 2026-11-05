package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.TeamManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TeamManagementController.class)
@ContextConfiguration(classes = {TeamManagementController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class TeamManagementControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TeamManagementService service;

    @Test void acceptsResponsibilityAndTeamWithoutChoosingAnAdherent() throws Exception {
        mvc.perform(put("/teams/events/20/responsibilities").with(user("coach@example.org").roles("ENCADRANT"))
                .contentType(MediaType.APPLICATION_JSON).content("[{\"tacheId\":3,\"equipeId\":10}]"))
                .andExpect(status().isOk());
        verify(service).responsibilities(20L,
                List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(3L, 10L)), "coach@example.org");
    }

    @Test void acceptsTheBrowserDatetimeLocalFormatAndEmptyOptionalFields() throws Exception {
        mvc.perform(post("/teams/equipes/2/events").with(user("coach@example.org").roles("ENCADRANT"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"equipes":[2],"type":"ACTIVITE","debut":"2026-11-04T18:00","fin":"2026-11-04T19:00",
                 "salleId":null,"lieu":null,"rendezVous":null,"adversaire":"","commentaire":""}
                """))
                .andExpect(status().isOk());
        verify(service).createEvent(eq(2L), argThat(e -> e.equipes().equals(List.of(2L))
                && e.debut().equals(LocalDateTime.of(2026, 11, 4, 18, 0))
                && e.fin().equals(LocalDateTime.of(2026, 11, 4, 19, 0)) && e.rendezVous() == null), eq("coach@example.org"));
    }

    @Test void rejectsMalformedDatesBeforeCallingTheService() throws Exception {
        mvc.perform(post("/teams/equipes/2/events").with(user("coach@example.org").roles("ENCADRANT"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"equipes":[2],"type":"ACTIVITE","debut":"04/11/2026 18:00","fin":"2026-11-04T19:00"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void returnsTheBusinessValidationMessageForTheReportedRequest() throws Exception {
        when(service.createEvent(eq(2L), any(), eq("coach@example.org")))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Le rendez-vous ne peut pas être postérieur au début de l’événement."));
        mvc.perform(post("/teams/equipes/2/events").with(user("coach@example.org").roles("ENCADRANT"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"equipes":[2,1],"type":"AUTRE","debut":"2026-11-04T18:00","fin":"2026-11-04T19:00",
                 "salleId":1,"lieu":"cdqsccsqcq","rendezVous":"2026-11-04T18:23","adversaire":"","commentaire":"cdcscqscqcsqc"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le rendez-vous ne peut pas être postérieur au début de l’événement."));
    }

    @Test void preservesAuthorizationStatusWhenProvidingAnErrorExplanation() throws Exception {
        when(service.createEvent(eq(2L), any(), anyString()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "Cette équipe ne vous est pas confiée"));
        mvc.perform(post("/teams/equipes/2/events").with(user("member@example.org").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"equipes":[2],"type":"ACTIVITE","debut":"2026-11-04T18:00","fin":"2026-11-04T19:00"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cette équipe ne vous est pas confiée"));
    }
}
