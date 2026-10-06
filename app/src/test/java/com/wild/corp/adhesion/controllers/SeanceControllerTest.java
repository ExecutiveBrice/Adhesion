package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.SeanceServices;
import com.wild.corp.adhesion.services.PresenceServices;
import com.wild.corp.adhesion.models.resources.PresencePrevueResponse;
import org.springframework.http.MediaType;
import com.wild.corp.adhesion.services.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SeanceController.class)
@ContextConfiguration(classes = {SeanceController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class SeanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeanceServices seanceServices;

    @MockitoBean
    private PresenceServices presenceServices;

    @Test
    void returnsPlannedAttendanceWithoutPrivateRegistrationDetails() throws Exception {
        when(presenceServices.getPresencesPrevues(15L, "member@example.org"))
                .thenReturn(List.of(new PresencePrevueResponse(22L, 9L, "Dupont", "Alice", null)));
        mockMvc.perform(get("/seance/15/presences-prevues").with(user("member@example.org").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adherentId").value(9))
                .andExpect(jsonPath("$[0].presencePrevue").isEmpty())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].paiementValide").doesNotExist());
    }

    @Test
    void updatesAttendanceUsingAuthenticatedIdentity() throws Exception {
        when(presenceServices.updatePresencePrevue(15L, false, "member@example.org"))
                .thenReturn(new PresencePrevueResponse(22L, 9L, "Dupont", "Alice", false));
        mockMvc.perform(patch("/seance/15/presence-prevue").with(user("member@example.org").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"presencePrevue\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presencePrevue").value(false));
        verify(presenceServices).updatePresencePrevue(15L, false, "member@example.org");
    }

    @Test
    void rejectsMissingOrNullAttendance() throws Exception {
        for (String body : List.of("{}", "{\"presencePrevue\":null}")) {
            mockMvc.perform(patch("/seance/15/presence-prevue").with(user("member@example.org").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void requiresAuthenticationForPlannedAttendance() throws Exception {
        mockMvc.perform(get("/seance/15/presences-prevues")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/seance/15/presence-prevue")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"presencePrevue\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void returnsSessionObjectsForSecretary() throws Exception {
        Presence presence = new Presence();
        presence.setId(22L);
        presence.setPresence(false);
        when(seanceServices.getSeancesForAdherent(
                java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 30), 9L))
                .thenReturn(List.of(new SeanceResponse(
                        15L, ESeance.PROGRAMMEE, null,
                        LocalDateTime.of(2026, 9, 8, 18, 0),
                        LocalDateTime.of(2026, 9, 8, 19, 0), null, null, presence, null)));

        mockMvc.perform(get("/seance")
                        .param("dateDebut", "2026-09-01")
                        .param("dateFin", "2026-09-30")
                        .param("adherentId", "9")
                        .with(user("secretariat@example.org").roles("SECRETAIRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].debut").value("2026-09-08T18:00:00"))
                .andExpect(jsonPath("$[0].presence.id").value(22))
                .andExpect(jsonPath("$[0].presence.presence").value(false));

        verify(seanceServices).getSeancesForAdherent(
                java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 30), 9L);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
