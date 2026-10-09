package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ComptaService;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.models.ComptaActivite;
import com.wild.corp.adhesion.models.resources.ComptaPeriode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = ComptaController.class)
@ContextConfiguration(classes = {ComptaController.class, ComptaControllerTest.SecurityTestConfiguration.class})
class ComptaControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ComptaService service;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void acceptsCalendarDatesAndASingleDayForAnAccountant() throws Exception {
        LocalDate day = LocalDate.of(2024, 2, 29);
        when(service.getAll(day, day)).thenReturn(List.of());
        mvc.perform(get("/compta/getAll").param("dateDebutPeriode", "2024-02-29")
                        .param("dateFinPeriode", "2024-02-29").with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isOk());
        verify(service).getAll(day, day);
    }

    @Test
    void returnsTheSummaryAndContributingPaymentsForTheSamePeriod() throws Exception {
        LocalDate day = LocalDate.of(2024, 2, 29);
        ComptaActivite recap = new ComptaActivite();
        recap.setNomActivite("Basket");
        recap.setHelloAsso(15);
        when(service.getPeriode(day, day)).thenReturn(new ComptaPeriode(List.of(recap), List.of(
                new ComptaPeriode.AdhesionCompta(7L, "Basket", "Martin", "Camille", "camille@example.org", "Validée", false,
                        List.of(new ComptaPeriode.PaiementCompta(8L, day, "HelloAsso", 15, true),
                                new ComptaPeriode.PaiementCompta(9L, day.minusDays(1), "HelloAsso", 100, false)), 15))));
        mvc.perform(get("/compta/periode").param("dateDebutPeriode", "2024-02-29")
                        .param("dateFinPeriode", "2024-02-29").with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recap[0].helloAsso").value(15))
                .andExpect(jsonPath("$.adhesions[0].nomAdherent").value("Martin"))
                .andExpect(jsonPath("$.adhesions[0].emailAdherent").value("camille@example.org"))
                .andExpect(jsonPath("$.adhesions[0].rapprochement").value(false))
                .andExpect(jsonPath("$.adhesions[0].paiements[0].dateReglement").value("2024-02-29"))
                .andExpect(jsonPath("$.adhesions[0].paiements[0].inclusDansPeriode").value(true))
                .andExpect(jsonPath("$.adhesions[0].paiements[1].inclusDansPeriode").value(false))
                .andExpect(jsonPath("$.adhesions[0].paiements[1].montant").value(100))
                .andExpect(jsonPath("$.adhesions[0].totalPeriode").value(15));
        verify(service).getPeriode(day, day);
    }

    @Test
    void rejectsMissingInvalidAndReversedDates() throws Exception {
        for (String path : List.of("/compta/getAll", "/compta/periode")) {
            mvc.perform(get(path).with(user("comptable").roles("COMPTABLE")))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path).param("dateDebutPeriode", "2026-02-30")
                            .param("dateFinPeriode", "2026-03-31").with(user("comptable").roles("COMPTABLE")))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path).param("dateDebutPeriode", "2026-10-01")
                            .param("dateFinPeriode", "2026-09-30").with(user("comptable").roles("COMPTABLE")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void deniesAccountingToAnOrdinaryUser() throws Exception {
        for (String path : List.of("/compta/getAll", "/compta/periode")) {
            mvc.perform(get(path).param("dateDebutPeriode", "2026-09-01")
                            .param("dateFinPeriode", "2026-09-30").with(user("adherent").roles("USER")))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void letsAnAccountantSaveAndDeletePaymentsWithAnAuditIdentity() throws Exception {
        mvc.perform(post("/compta/adhesions/7/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":8,\"montant\":15,\"dateReglement\":\"2026-09-30\",\"typeReglement\":\"CB\"}")
                        .with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isNoContent());
        verify(service).savePaiement(7L, new ComptaPeriode.PaiementCompta(8L,
                LocalDate.of(2026, 9, 30), "CB", 15), "comptable");
        mvc.perform(delete("/compta/adhesions/7/paiements/8").with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isNoContent());
        verify(service).deletePaiement(7L, 8L, "comptable");
    }

    @Test
    void rejectsOrdinaryUserPaymentWrites() throws Exception {
        mvc.perform(post("/compta/adhesions/7/paiements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":8,\"montant\":15,\"dateReglement\":\"2026-09-30\",\"typeReglement\":\"CB\"}")
                        .with(user("adherent").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/compta/adhesions/7/paiements/8").with(user("adherent").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void letsAnAccountantSaveBothReconciliationChoicesWithAnAuditIdentity() throws Exception {
        for (boolean value : List.of(true, false)) {
            mvc.perform(put("/compta/adhesions/7/rapprochement").param("rapprochement", String.valueOf(value))
                            .with(user("comptable").roles("COMPTABLE")))
                    .andExpect(status().isNoContent());
            verify(service).updateRapprochement(7L, value, "comptable");
        }
    }

    @Test
    void rejectsMissingInvalidAndUnauthorizedReconciliationWrites() throws Exception {
        mvc.perform(put("/compta/adhesions/7/rapprochement").with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/compta/adhesions/7/rapprochement").param("rapprochement", "invalid")
                        .with(user("comptable").roles("COMPTABLE")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/compta/adhesions/7/rapprochement").param("rapprochement", "true")
                        .with(user("adherent").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated()).build();
        }
    }
}
