package com.wild.corp.adhesion.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.corp.adhesion.models.ConfirmationTokenType;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.models.PwaSessionToken;
import com.wild.corp.adhesion.repository.ConfirmationTokenRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.repository.PwaSessionTokenRepository;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig
@ContextConfiguration(classes = PasswordResetMockMvcTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "server.name=example.test")
class PwaSessionMockMvcTest {
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private ConfirmationTokenRepository confirmationTokens;
    @Autowired private PwaSessionTokenRepository tokens;
    @Autowired private ConfirmationTokenService tokenService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private PasswordResetService passwordReset;
    @MockitoBean private EmailService emailService;
    @MockitoBean private UserServices userServices;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private SurrogateService surrogateService;

    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        tokens.deleteAll();
        confirmationTokens.deleteAll();
        users.deleteAll();
        user = users.saveAndFlush(new User("member@example.org", passwords.encode("Ancien!Secret42")));
        UserDetails principal = UserDetails.build(user);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtUtils.generateJwtToken(any())).thenReturn("access-token");
    }

    @Test
    void onlyRememberedLoginsIssueAHashedPwaToken() throws Exception {
        mvc.perform(post("/auth/signin").contentType("application/json")
                        .content("{\"username\":\"member@example.org\",\"password\":\"secret\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isEmpty());
        assertThat(tokens.findAll()).isEmpty();
        String secret = login();
        var stored = tokens.findAll().getFirst();
        assertThat(stored.getTokenHash()).isEqualTo(ConfirmationTokenService.hash(secret)).isNotEqualTo(secret);
        assertThat(stored.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(89)));
    }

    @Test
    void refreshRotatesTheSecretAndRejectsReuse() throws Exception {
        String first = login();
        var result = refresh(first).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.username").value(user.getUsername())).andReturn();
        String second = new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(second).isNotEqualTo(first);
        refresh(first).andExpect(status().isUnauthorized());
        refresh(second).andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredAndWrongTypeTokens() throws Exception {
        String expired = "expired-secret";
        PwaSessionToken session = new PwaSessionToken();
        session.setUser(user);
        session.setTokenHash(ConfirmationTokenService.hash(expired));
        session.setExpiresAt(Instant.now().minusSeconds(60));
        tokens.saveAndFlush(session);
        String email = tokenService.create(user, ConfirmationTokenType.EMAIL_CONFIRMATION,
                Duration.ofMinutes(10), Instant.now());
        refresh(expired).andExpect(status().isUnauthorized());
        refresh(email).andExpect(status().isUnauthorized());
        refresh("unknown").andExpect(status().isUnauthorized());
    }

    @Test
    void signoutRevokesTheSecretAndIsIdempotent() throws Exception {
        String secret = login();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/auth/signout").contentType("application/json")
                            .content(body(secret))).andExpect(status().isNoContent());
        }
        refresh(secret).andExpect(status().isUnauthorized());
    }

    @Test
    void resettingThePasswordPreventsAutomaticReconnection() throws Exception {
        String secret = login();
        String reset = tokenService.create(user, ConfirmationTokenType.PASSWORD_RESET,
                Duration.ofMinutes(10), Instant.now());
        passwordReset.confirm(reset, "Nouveau!Secret42");
        refresh(secret).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyOneConcurrentRefreshCanUseTheSecret() throws Exception {
        String secret = login();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentRefresh(secret, ready, start));
            var second = executor.submit(() -> concurrentRefresh(secret, ready, start));
            ready.await();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 401);
        }
    }

    private int concurrentRefresh(String secret, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return refresh(secret).andReturn().getResponse().getStatus();
    }

    private String login() throws Exception {
        var result = mvc.perform(post("/auth/signin").contentType("application/json")
                        .content("{\"username\":\"member@example.org\",\"password\":\"secret\",\"rememberSession\":true}"))
                .andExpect(status().isOk()).andReturn();
        return new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("refreshToken").asText();
    }

    private ResultActions refresh(String secret) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType("application/json").content(body(secret)));
    }

    private String body(String secret) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of("refreshToken", secret));
    }
}
