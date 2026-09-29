package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.resources.AdherentLite;
import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.RappelServices;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.services.UserServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = AdherentController.class)
@ContextConfiguration(classes = {AdherentController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
class AdherentEmailAuthorizationMvcTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdherentServices adherentServices;
    @MockitoBean
    private UserServices userServices;
    @MockitoBean
    private RappelServices rappelServices;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void returnsForbiddenRatherThanConflictWhenEmailChangeIsDenied() throws Exception {
        given(adherentServices.update(any(AdherentLite.class)))
                .willThrow(new AccessDeniedException("Modification e-mail interdite"));

        mockMvc.perform(post("/adherent/update").with(user("actor@example.test").roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\":42,\"user\":{\"username\":\"new@example.test\"}}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "SECRETAIRE", "MEMBRECA", "ADMIN"})
    void allowsUpdatesForEachSupportedRoleWithoutRequiringAnAdditionalUserRole(String role) throws Exception {
        mockMvc.perform(post("/adherent/update").with(user("actor@example.test").roles(role))
                        .contentType(APPLICATION_JSON).content("{\"id\":42}"))
                .andExpect(status().isOk());
    }
}
