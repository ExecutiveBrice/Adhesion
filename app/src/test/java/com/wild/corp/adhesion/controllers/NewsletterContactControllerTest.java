package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.NewsletterSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsletterContactController.class)
@ContextConfiguration(classes = {NewsletterContactController.class, NewsletterCampaignControllerTest.SecurityConfiguration.class})
class NewsletterContactControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NewsletterSyncService service;

    @Test
    void onlyAdminsCanStartOrReadSynchronization() throws Exception {
        mvc.perform(post("/newsletter/contacts/sync")).andExpect(status().isUnauthorized());
        mvc.perform(get("/newsletter/contacts/sync")).andExpect(status().isUnauthorized());
        for (String role : List.of("USER", "SECRETAIRE", "MEMBRECA", "BUREAU", "ENCADRANT", "COMPTABLE", "LOGISTIQUE")) {
            mvc.perform(post("/newsletter/contacts/sync").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(get("/newsletter/contacts/sync").with(user("member").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
        var progress = new NewsletterSyncService.Progress(true, 10, 2, 1, 1, 0, 0, 0, "");
        when(service.start()).thenReturn(progress);
        when(service.status()).thenReturn(progress);
        mvc.perform(post("/newsletter/contacts/sync").with(user("admin").roles("ADMIN")))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.running").value(true));
        mvc.perform(get("/newsletter/contacts/sync").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.processed").value(2));
    }
}
