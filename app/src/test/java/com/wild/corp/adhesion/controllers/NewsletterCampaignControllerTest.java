package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.repository.HistoriqueRepository;
import com.wild.corp.adhesion.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {NewsletterCampaignController.class, MailController.class})
@ContextConfiguration(classes = {NewsletterCampaignController.class, MailController.class, NewsletterCampaignControllerTest.SecurityConfiguration.class})
class NewsletterCampaignControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NewsletterCampaignService campaigns;
    @MockitoBean EmailService mail;
    @MockitoBean AdherentServices adherents;
    @MockitoBean HistoriqueRepository history;

    @Test
    void preservesExistingSendPermissionsOnEveryNewsletterEndpoint() throws Exception {
        mvc.perform(get("/newsletter/campaigns")).andExpect(status().isUnauthorized());
        for (String role : List.of("USER", "ENCADRANT", "COMPTABLE", "LOGISTIQUE")) {
            mvc.perform(get("/newsletter/campaigns").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(get("/newsletter/campaigns/7").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(get("/newsletter/campaigns/7/status").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post("/newsletter/campaigns/7/send").with(user("member").roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"confirmationVersion\":\"v1\"}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(campaigns);
        Historique accepted = new Historique(); accepted.setCampaignId(7L); accepted.setCampaignStatus("ACCEPTED");
        when(campaigns.send(7L, "v1")).thenReturn(accepted);
        when(campaigns.status(7L)).thenReturn(accepted);
        for (String role : List.of("SECRETAIRE", "BUREAU", "MEMBRECA", "ADMIN")) {
            mvc.perform(get("/newsletter/campaigns").with(user("sender").roles(role))).andExpect(status().isOk());
            mvc.perform(get("/newsletter/campaigns/7").with(user("sender").roles(role))).andExpect(status().isOk());
            mvc.perform(get("/newsletter/campaigns/7/status").with(user("sender").roles(role))).andExpect(status().isOk());
            mvc.perform(post("/newsletter/campaigns/7/send").with(user("sender").roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"confirmationVersion\":\"v1\"}"))
                    .andExpect(status().isAccepted()).andExpect(jsonPath("$.campaignStatus").value("ACCEPTED"));
        }
    }

    @Test
    void allowsBrowserPreflightWithAuthorizationAndReturnsCorsHeaders() throws Exception {
        for (String path : List.of("/newsletter/campaigns?ngsw-bypass=true", "/newsletter/campaigns/7",
                "/newsletter/campaigns/7/status", "/newsletter/campaigns/7/send")) {
            String method = path.endsWith("/send") ? "POST" : "GET";
            mvc.perform(options(path).header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", method)
                    .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(header().string("Access-Control-Allow-Methods", method))
                    .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"));
        }
        verifyNoInteractions(campaigns);
        when(campaigns.drafts()).thenReturn(List.of());
        mvc.perform(get("/newsletter/campaigns?ngsw-bypass=true").header("Origin", "http://localhost:4200")
                .with(user("sender").roles("BUREAU")))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(content().json("[]"));
    }

    @Test
    void upstreamErrorsHaveAnExplicitBodyWithoutAnErrorDispatchOrAuthenticationFailure() throws Exception {
        for (HttpStatus failure : List.of(HttpStatus.BAD_GATEWAY, HttpStatus.CONFLICT, HttpStatus.SERVICE_UNAVAILABLE)) {
            doThrow(new ResponseStatusException(failure,
                    "Brevo : lecture des brouillons impossible (HTTP 401). Vérifiez la clé API Brevo.")).when(campaigns).drafts();
            mvc.perform(get("/newsletter/campaigns").header("Origin", "http://localhost:4200")
                    .with(user("sender").roles("BUREAU")))
                    .andExpect(status().is(failure.value()))
                    .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(jsonPath("$.status").value(failure.value()))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("HTTP 401")))
                    .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getErrorMessage()).isNull())
                    .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getForwardedUrl()).isNull());
        }
    }

    @Test
    void removesTemplateTwoButPreservesConvocationsAndManualMailAndDisabledSetting() throws Exception {
        when(mail.isMailSendingEnabled()).thenReturn(true);
        mvc.perform(post("/email/sendTemplate?templateId=2").contentType(MediaType.APPLICATION_JSON)
                .content("[]").with(user("sender").roles("BUREAU"))).andExpect(status().isGone());
        verify(mail, never()).diffusionTemplate(any(), eq(2L));
        mvc.perform(post("/email/sendTemplate?templateId=6").contentType(MediaType.APPLICATION_JSON)
                .content("[]").with(user("sender").roles("BUREAU"))).andExpect(status().isOk());
        verify(mail).diffusionTemplate(List.of(), 6L);
        mvc.perform(post("/email/").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Activités\",\"text\":\"Bonjour\",\"diffusion\":[]}")
                .with(user("sender").roles("BUREAU"))).andExpect(status().isOk());
        verify(mail).sendMessage(any());
        when(mail.isMailSendingEnabled()).thenReturn(false);
        mvc.perform(post("/email/sendTemplate?templateId=6").contentType(MediaType.APPLICATION_JSON)
                .content("[]").with(user("sender").roles("BUREAU"))).andExpect(status().isServiceUnavailable());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityConfiguration {
        @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }
    }
}
