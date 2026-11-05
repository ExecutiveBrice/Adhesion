package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.GoogleAgendaServices;
import com.wild.corp.adhesion.services.SeanceServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActiviteController.class)
@ContextConfiguration(classes = {ActiviteController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class ActiviteDetailControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ActiviteServices activities;
    @MockitoBean SeanceServices events;
    @MockitoBean GoogleAgendaServices google;

    @Test
    void onlyAdministratorsAndSecretariesCanReadTheUnscopedDetail() throws Exception {
        Activite activity = new Activite();
        activity.setId(7L);
        activity.setNom("Danse");
        when(activities.getDetail(7L)).thenReturn(activity);
        for (String role : new String[]{"ADMIN", "SECRETAIRE"}) {
            mvc.perform(get("/activite/7").with(user("gestion@example.org").roles(role)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Danse"));
        }
        mvc.perform(get("/activite/7").with(user("membre@example.org").roles("USER")))
                .andExpect(status().isForbidden());
        verify(activities, times(2)).getDetail(7L);
    }

    @Test
    void managedDetailUsesTheAuthenticatedAccountToRestrictTheActivity() throws Exception {
        Activite activity = new Activite();
        activity.setId(7L);
        when(activities.getManagedDetail("referent@example.org", 7L)).thenReturn(activity);
        mvc.perform(get("/activite/managed/7").with(user("referent@example.org").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(activities).getManagedDetail("referent@example.org", 7L);
        verify(activities, never()).getDetail(any());
    }

    @Test
    void coachesUseTheScopedListingWithTheAuthenticatedAccountAndFilters() throws Exception {
        when(activities.getManagedPage(eq("coach@example.org"), eq("Danse"), isNull(), isNull(),
                isNull(), isNull(), isNull(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/activite/managed/page").param("search", "Danse")
                        .with(user("coach@example.org").roles("ENCADRANT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        verify(activities).getManagedPage(eq("coach@example.org"), eq("Danse"), isNull(), isNull(),
                isNull(), isNull(), isNull(), any());
        mvc.perform(get("/activite/page").with(user("coach@example.org").roles("ENCADRANT")))
                .andExpect(status().isForbidden());
    }
}
