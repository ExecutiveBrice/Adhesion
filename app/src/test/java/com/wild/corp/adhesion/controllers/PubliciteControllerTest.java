package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.repository.PubliciteRepository;
import com.wild.corp.adhesion.services.PubliciteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PubliciteController.class)
@ContextConfiguration(classes = {PubliciteController.class, MaintenanceAuthorizationMvcTest.SecurityTestConfiguration.class})
class PubliciteControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PubliciteService service;
    private final MockMultipartFile file = new MockMultipartFile("file", "pub.png", "image/png", new byte[]{1});

    @Test
    void anonymousCannotReadOrManageAdvertisements() throws Exception {
        mvc.perform(get("/publicites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/publicites/1/image")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/publicites").file(file).param("description", "Pub")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/publicites/1").file(file).param("description", "Pub")
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isUnauthorized());
        mvc.perform(put("/publicites/1/settings").contentType("application/json")
                .content("{\"displayOrder\":2,\"active\":false}" )).andExpect(status().isUnauthorized());
        mvc.perform(delete("/publicites/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "SECRETAIRE", "COMPTABLE"})
    void otherRolesCanViewButCannotPublishOrDelete(String role) throws Exception {
        var member = user("member").roles(role);
        when(service.image(1, "member", false)).thenReturn(new PubliciteRepository.Image("image/png", new byte[]{1, 2}));
        mvc.perform(get("/publicites").with(member)).andExpect(status().isOk());
        mvc.perform(get("/publicites/1/image").with(member)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andExpect(content().bytes(new byte[]{1, 2}));
        mvc.perform(multipart("/publicites").file(file).param("description", "Pub").with(member)).andExpect(status().isForbidden());
        mvc.perform(multipart("/publicites/1").file(file).param("description", "Pub")
                .param("displayOrder", "1").param("active", "true").with(member)
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isForbidden());
        mvc.perform(put("/publicites/1/settings").contentType("application/json")
                .content("{\"displayOrder\":2,\"active\":false}").with(member)).andExpect(status().isForbidden());
        mvc.perform(delete("/publicites/1").with(member)).andExpect(status().isForbidden());
        verify(service, never()).create(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(service, never()).update(anyLong(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(service, never()).updateSettings(anyLong(), any(), any());
        verify(service, never()).delete(anyLong());
    }

    @Test
    void globalCommunicationCanPublishAndDelete() throws Exception {
        var communication = user("communication").roles("COMMUNICATION_GLOBAL");
        when(service.create(eq("Affiche"), eq("https://facebook.com/alod"),
                eq("https://instagram.com/alod"), eq("https://alod.fr"), eq(List.of(2L, 4L)), eq(5), eq(false), any()))
                .thenReturn(new PubliciteRepository.Publicite(7L, "Affiche", "https://facebook.com/alod",
                        "https://instagram.com/alod", "https://alod.fr", List.of(2L, 4L), 5, false));
        mvc.perform(multipart("/publicites").file(file).param("description", "Affiche")
                        .param("facebookUrl", "https://facebook.com/alod")
                        .param("instagramUrl", "https://instagram.com/alod")
                        .param("websiteUrl", "https://alod.fr").param("sectionIds", "2", "4")
                        .param("displayOrder", "5").param("active", "false").with(communication))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.facebookUrl").value("https://facebook.com/alod"))
                .andExpect(jsonPath("$.instagramUrl").value("https://instagram.com/alod"))
                .andExpect(jsonPath("$.sectionIds[1]").value(4))
                .andExpect(jsonPath("$.displayOrder").value(5))
                .andExpect(jsonPath("$.active").value(false));
        when(service.updateSettings(7, 1, true)).thenReturn(new PubliciteRepository.Publicite(7L, "Affiche",
                null, null, null, List.of(2L, 4L), 1, true));
        mvc.perform(put("/publicites/7/settings").contentType("application/json")
                        .content("{\"displayOrder\":1,\"active\":true}").with(communication))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.active").value(true));
        when(service.update(eq(7L), eq("Affiche modifiée"), isNull(), isNull(), eq("https://alod.fr"),
                eq(List.of(4L)), eq(3), eq(true), isNull())).thenReturn(new PubliciteRepository.Publicite(7L,
                "Affiche modifiée", null, null, "https://alod.fr", List.of(4L), 3, true));
        mvc.perform(multipart("/publicites/7").param("description", "Affiche modifiée")
                        .param("websiteUrl", "https://alod.fr").param("sectionIds", "4")
                        .param("displayOrder", "3").param("active", "true").with(communication)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Affiche modifiée"))
                .andExpect(jsonPath("$.sectionIds[0]").value(4));
        mvc.perform(delete("/publicites/7").with(communication)).andExpect(status().isNoContent());
        verify(service).delete(7);
    }
}
