package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.PasswordResetService;
import com.wild.corp.adhesion.services.SurrogateService;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.services.UserServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = {AuthController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
class SignupAuthorizationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServices userServices;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private PasswordResetService passwordResetService;
    @MockitoBean
    private SurrogateService surrogateService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void anonymousVisitorCanCreateAnAccount() throws Exception {
        given(userServices.existsByEmail(anyString())).willReturn(false);

        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"new.member@example.org\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk());
    }

}
