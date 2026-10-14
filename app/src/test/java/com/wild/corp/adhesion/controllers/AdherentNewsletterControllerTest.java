package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Duration;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdherentNewsletterController.class)
@ContextConfiguration(classes = {AdherentNewsletterController.class, NewsletterCampaignControllerTest.SecurityConfiguration.class})
class AdherentNewsletterControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdherentNewsletterService service;

    @Test
    void authenticatesAndValidatesPreferenceBeforeInvokingService() throws Exception {
        mvc.perform(get("/adherent/7/newsletter")).andExpect(status().isUnauthorized());
        mvc.perform(put("/adherent/7/newsletter").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subscribed\":true,\"expectedEmail\":\"owner@example.org\"}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/adherent/7/newsletter").with(user("owner").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void allowsOwnersAndEnforcesObjectPermissionsFromService() throws Exception {
        var subscribed = new AdherentNewsletterService.Subscription("owner@example.org", NewsletterContactService.Status.SUBSCRIBED, true, true, false);
        when(service.status(eq(7L), any())).thenReturn(subscribed);
        when(service.update(eq(7L), any(), eq(true), eq("owner@example.org"))).thenReturn(subscribed);
        mvc.perform(get("/adherent/7/newsletter").with(user("owner").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBSCRIBED"));
        mvc.perform(put("/adherent/7/newsletter").with(user("owner").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"subscribed\":true,\"expectedEmail\":\"owner@example.org\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canModify").value(true));
        when(service.status(eq(8L), any())).thenThrow(new AccessDeniedException("denied"));
        mvc.perform(get("/adherent/8/newsletter").with(user("owner").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void upstreamErrorsDoNotExpireUserSessionAndExposeRetryDelay() throws Exception {
        when(service.status(eq(7L), any())).thenThrow(new NewsletterApiException(401, Duration.ZERO));
        mvc.perform(get("/adherent/7/newsletter").with(user("owner").roles("USER")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.detail").exists());
        when(service.status(eq(7L), any())).thenThrow(new NewsletterApiException(429, Duration.ofSeconds(60)));
        mvc.perform(get("/adherent/7/newsletter").with(user("owner").roles("USER")))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "60"));
    }
}
