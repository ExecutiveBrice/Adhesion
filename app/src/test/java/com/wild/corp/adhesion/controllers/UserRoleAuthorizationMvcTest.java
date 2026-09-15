package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.PresenceServices;
import com.wild.corp.adhesion.services.SeanceServices;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.services.UserServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = {UserController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
class UserRoleAuthorizationMvcTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserServices userServices;
    @MockitoBean private PresenceServices presenceServices;
    @MockitoBean private SeanceServices seanceServices;
    @MockitoBean private AdhesionServices adhesionServices;
    @MockitoBean private SeanceRepository seanceRepository;
    @MockitoBean private AdherentRepository adherentRepository;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Test
    void encadrantCanReadTheirSessions() throws Exception {
        mockMvc.perform(get("/user/seancesDuJour")
                .with(user("encadrant@example.test").roles("ENCADRANT")))
                .andExpect(status().isOk());
        verify(userServices).getSeancesDuJourForUser("encadrant@example.test");
    }

    @Test
    void oldProfAuthorityNoLongerGrantsAccess() throws Exception {
        mockMvc.perform(get("/user/seancesDuJour")
                .with(user("prof@example.test").roles("PROF")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userServices);
    }

    @Test
    void secretaryCanGrantOrdinaryRole() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("secretary@example.test").roles("SECRETAIRE"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_ENCADRANT"))
                .andExpect(status().isOk());
        verify(userServices).findByEmail("member@example.test");
        verify(userServices).grantUser(eq(ERole.ROLE_ENCADRANT), any());
    }

    @Test
    void secretaryCanGrantMembreCaRole() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("secretary@example.test").roles("SECRETAIRE"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_MEMBRECA"))
                .andExpect(status().isOk());
        verify(userServices).grantUser(eq(ERole.ROLE_MEMBRECA), any());
    }

    @Test
    void secretaryCannotGrantSiteAdministratorRole() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("secretary@example.test").roles("SECRETAIRE"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_ADMIN"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userServices);
    }

    @Test
    void membreCaCannotChangeRoles() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("member@example.test").roles("MEMBRECA"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_ENCADRANT"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userServices);
    }

    @Test
    void siteAdministratorCanGrantSiteAdministratorRole() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("admin@example.test").roles("ADMIN"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_ADMIN"))
                .andExpect(status().isOk());
        verify(userServices).grantUser(eq(ERole.ROLE_ADMIN), any());
    }

    @Test
    void ordinaryUserCannotChangeRoles() throws Exception {
        mockMvc.perform(post("/user/grantUser").with(user("member@example.test").roles("USER"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_ENCADRANT"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userServices);
    }

    @Test
    void userRoleCannotBeRemoved() throws Exception {
        mockMvc.perform(post("/user/unGrantUser").with(user("admin@example.test").roles("ADMIN"))
                .param("userEmail", "member@example.test")
                .contentType(APPLICATION_JSON).content("ROLE_USER"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userServices);
    }
}
