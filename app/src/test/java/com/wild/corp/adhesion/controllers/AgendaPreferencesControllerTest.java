package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.resources.AgendaPreferences;
import com.wild.corp.adhesion.services.AgendaPreferencesServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgendaPreferencesController.class)
@ContextConfiguration(classes = {AgendaPreferencesController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class AgendaPreferencesControllerTest {
    private static final String URL = "/adherent/me/agenda-preferences";
    @Autowired private MockMvc mvc;
    @MockitoBean private AgendaPreferencesServices preferences;

    @Test
    void readsAndUpdatesOnlyTheAuthenticatedProfile() throws Exception {
        var selection = new AgendaPreferences(Set.of(10L), Set.of(8L));
        when(preferences.get("alice@example.test")).thenReturn(selection);
        when(preferences.update("alice@example.test", selection)).thenReturn(selection);
        mvc.perform(get(URL).with(user("alice@example.test").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sectionsMasquees[0]").value(10));
        mvc.perform(put(URL).with(user("alice@example.test").roles("USER"))
                        .param("adherentId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionsMasquees\":[10],\"agendasGoogleMasques\":[8]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agendasGoogleMasques[0]").value(8));
        verify(preferences).update("alice@example.test", selection);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectionsMasquees\":[],\"agendasGoogleMasques\":[]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(preferences);
    }

    @Test
    void rejectsIncompleteOrInvalidSelections() throws Exception {
        for (String body : List.of("{}", "{\"sectionsMasquees\":null,\"agendasGoogleMasques\":[]}",
                "{\"sectionsMasquees\":[-1],\"agendasGoogleMasques\":[]}",
                "{\"sectionsMasquees\":[],\"agendasGoogleMasques\":[0]}",
                "{\"sectionsMasquees\":[null],\"agendasGoogleMasques\":[]}")) {
            mvc.perform(put(URL).with(user("alice@example.test").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(preferences);
    }
}
