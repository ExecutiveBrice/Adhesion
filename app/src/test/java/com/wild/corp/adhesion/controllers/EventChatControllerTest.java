package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.EventChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventChatController.class)
@ContextConfiguration(classes = {EventChatController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class EventChatControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EventChatService chat;

    @Test void loadsTheHistoryOfTheRequestedEventWithTheAuthenticatedUserAndCursor() throws Exception {
        when(chat.history(20L, null, 40L, "alice")).thenReturn(List.of(
                new EventChatService.Message(41L, 1L, "Alice", "Bonjour", Instant.parse("2026-11-09T15:00:00Z"))));
        mvc.perform(get("/teams/events/20/messages?afterId=40").with(user("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("Bonjour"));
        verify(chat).history(20L, null, 40L, "alice");
    }

    @Test void sendsAsTheAuthenticatedUserRatherThanAcceptingAnAuthorFromTheBrowser() throws Exception {
        mvc.perform(post("/teams/events/20/messages").with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Bonjour\"}"))
                .andExpect(status().isOk());
        verify(chat).send(20L, "Bonjour", "alice");
    }

    @Test void rejectsUnauthenticatedReadAndWriteRequests() throws Exception {
        mvc.perform(get("/teams/events/20/messages")).andExpect(status().isUnauthorized());
        mvc.perform(post("/teams/events/20/messages").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Bonjour\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chat);
    }
}
