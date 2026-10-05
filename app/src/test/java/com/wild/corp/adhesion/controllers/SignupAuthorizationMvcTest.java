package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.PasswordResetService;
import com.wild.corp.adhesion.services.PwaSessionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;

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
    private PwaSessionService pwaSessionService;
    @MockitoBean
    private com.wild.corp.adhesion.services.TribuSessionService tribuSessionService;
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

    @Test
    void refreshAndSignoutDoNotRequireAnUnexpiredAccessToken() throws Exception {
        mockMvc.perform(post("/auth/refresh").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"session-secret\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/signout").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"session-secret\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void localAngularFrontendCanCallTheApiAcrossOrigins() throws Exception {
        mockMvc.perform(options("/publicites")
                        .header(ORIGIN, "http://localhost:4200")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    @Test
    void actualResponsesContainCorsHeadersAndUnknownOriginsAreRejected() throws Exception {
        given(userServices.existsByEmail(anyString())).willReturn(false);
        String body = "{\"username\":\"new.member@example.org\",\"password\":\"secret1\"}";

        mockMvc.perform(post("/auth/signup").header(ORIGIN, "http://localhost:4200")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

        mockMvc.perform(options("/publicites")
                        .header(ORIGIN, "https://malicious.example")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

}
