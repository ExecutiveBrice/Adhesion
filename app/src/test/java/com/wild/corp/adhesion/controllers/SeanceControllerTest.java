package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.SeanceServices;
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
                        LocalDateTime.of(2026, 9, 8, 19, 0), null, null, presence)));

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
