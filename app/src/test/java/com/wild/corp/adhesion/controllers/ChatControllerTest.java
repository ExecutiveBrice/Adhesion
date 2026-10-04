package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ChatService;
import com.wild.corp.adhesion.services.ParamBooleanServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@ContextConfiguration(classes = {ChatController.class, SeanceControllerTest.SecurityTestConfiguration.class})
class ChatControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ChatService chat;
    @MockitoBean private ParamBooleanServices paramBooleanServices;

    @BeforeEach
    void enableChat() {
        when(paramBooleanServices.findByParamValue("Show_Chat")).thenReturn(true);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/chat")).andExpect(status().isUnauthorized());
        mvc.perform(get("/chat/7/messages")).andExpect(status().isUnauthorized());
        mvc.perform(post("/chat/7/read").contentType("application/json")
                .content("{\"lastMessageId\":10}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/chat/7/messages").contentType("application/json")
                .content("{\"content\":\"Bonjour\"}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(chat);
    }

    @Test
    void allowsOrdinaryMemberAndUsesAuthenticatedIdentity() throws Exception {
        when(chat.send("alice", 7L, "Bonjour")).thenReturn(
                new ChatService.Message(1L, 4L, "Alice", null, "Bonjour", Instant.parse("2026-10-02T10:00:00Z")));
        mvc.perform(post("/chat/7/messages").with(user("alice").roles("USER"))
                        .contentType("application/json").content("{\"content\":\"Bonjour\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senderUserId").value(4));
        verify(chat).send("alice", 7L, "Bonjour");
    }

    @Test
    void rejectsRequestsWhenChatIsDisabled() throws Exception {
        when(paramBooleanServices.findByParamValue("Show_Chat")).thenReturn(false);

        mvc.perform(get("/chat").with(user("alice")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(chat);
    }

    @Test
    void rejectsInvalidBodyBeforeService() throws Exception {
        for (String body : new String[] {"{}", "{\"content\":\"  \"}", "{\"content\":\"" + "x".repeat(2001) + "\"}"}) {
            mvc.perform(post("/chat/7/messages").with(user("alice"))
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(chat);
    }

    @Test
    void acknowledgesMessagesWithAuthenticatedIdentityAndValidatesCursor() throws Exception {
        when(chat.markRead("alice", 7L, 10L)).thenReturn(new ChatService.Unread(2));
        mvc.perform(post("/chat/7/read").with(user("alice"))
                .contentType("application/json").content("{\"lastMessageId\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(2));
        verify(chat).markRead("alice", 7L, 10L);
        for (String body : new String[] {"{}", "{\"lastMessageId\":0}", "{\"lastMessageId\":-1}"}) {
            mvc.perform(post("/chat/7/read").with(user("alice"))
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        verifyNoMoreInteractions(chat);
    }
}
