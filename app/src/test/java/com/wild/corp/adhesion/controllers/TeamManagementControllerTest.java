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

    @Test void loadsOneEventByIdWithTheAuthenticatedUser() throws Exception {
        mvc.perform(get("/teams/events/20").with(user("member@example.org"))).andExpect(status().isOk());
        verify(service).eventDetails(20L, "member@example.org");
    }

    @Test void acceptsRosterChangesWithoutAFutureEventsOption() throws Exception {
        mvc.perform(put("/teams/activities/7/equipes/10/effectif").with(user("manager@example.org"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"adhesions\":[100,101]}"))
                .andExpect(status().isOk());
        verify(service).roster(7L, 10L, new TeamManagementService.RosterEdit(List.of(100L, 101L)), "manager@example.org");
    }

    @Test void acceptsAllThreeExplicitInvitationAnswers() throws Exception {
        for (String value : List.of("true", "false", "null")) {
            mvc.perform(patch("/teams/events/20/invitations/30/reply").with(user("member@example.org"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"value\":" + value + "}"))
                    .andExpect(status().isOk());
        }
        verify(service).reply(20L, 30L, true, "member@example.org");
        verify(service).reply(20L, 30L, false, "member@example.org");
        verify(service).reply(20L, 30L, null, "member@example.org");
        verify(service, never()).attendance(anyLong(), anyLong(), anyBoolean(), anyString());
    }

    @Test void sendsIndividualCommandsWithTheEventAndInvitationIds() throws Exception {
        mvc.perform(post("/teams/events/20/invitations/30/publish").with(user("coach@example.org")))
                .andExpect(status().isOk());
        mvc.perform(post("/teams/events/20/invitations/30/remind").with(user("coach@example.org"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"requestId\":\"request-key\"}"))
                .andExpect(status().isOk());
        verify(service).publishInvitation(20L, 30L, "coach@example.org");
        verify(service).remindInvitation(20L, 30L, "request-key", "coach@example.org");
        verify(service, never()).publish(anyLong(), anyString());
        verify(service, never()).remind(anyLong(), anyString(), anyString());
    }

    @Test void rejectsAnOmittedAnswerAndAnUndecidedActualAttendance() throws Exception {
        for (String route : List.of("reply", "attendance")) {
            mvc.perform(patch("/teams/events/20/invitations/30/" + route).with(user("member@example.org"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/teams/events/20/invitations/30/attendance").with(user("coach@example.org"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":null}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void loadsMemberDetailsInTheSelectedActivityForTheAuthenticatedUser() throws Exception {
        var person = new TeamManagementService.Person(1L, "Alice", "Martin", 100L, "Validée", true, true, false);
        when(service.memberDetails(7L, 100L, "manager@example.org"))
                .thenReturn(new TeamManagementService.MemberDetails(person, null, "alice@example.org", null, null, null, null, null));
        mvc.perform(get("/teams/activities/7/adhesions/100").with(user("manager@example.org").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personne.prenom").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.org"));
        verify(service).memberDetails(7L, 100L, "manager@example.org");
    }

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
