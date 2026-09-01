package com.wild.corp.adhesion.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.corp.adhesion.models.ConfirmationToken;
import com.wild.corp.adhesion.models.ConfirmationTokenType;
import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.ConfirmationTokenRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.ConfirmationTokenService;
import com.wild.corp.adhesion.services.EmailService;
import com.wild.corp.adhesion.services.PasswordResetRateLimiter;
import com.wild.corp.adhesion.services.PasswordResetService;
import com.wild.corp.adhesion.services.PasswordResetWorker;
import com.wild.corp.adhesion.services.SurrogateService;
import com.wild.corp.adhesion.services.UserServices;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@ContextConfiguration(classes = PasswordResetMockMvcTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "server.name=example.test",
        "adhesion.security.password-reset.token-lifetime-minutes=30",
        "adhesion.security.password-reset.rate-limit.ip=1000",
        "adhesion.security.password-reset.rate-limit.address=1000"
})
class PasswordResetMockMvcTest {
    private static final String STRONG_PASSWORD = "Nouveau!Secret42";
    private static final Pattern TOKEN_IN_EMAIL = Pattern.compile("resetPassword/([A-Za-z0-9_-]+)");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConfirmationTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private UserServices userServices;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private SurrogateService surrogateService;

    private MockMvc mockMvc;
    private final List<EmailContent> sentEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        sentEmails.clear();
        doAnswer(invocation -> {
            sentEmails.add(invocation.getArgument(0));
            return null;
        }).when(emailService).sendMessage(any(EmailContent.class));
    }

    @Test
    void requestHasTheSameAcceptedResponseAndNeverReturnsTheToken() throws Exception {
        createUser("member@example.org");

        MvcResult existing = requestReset("member@example.org", "192.0.2.1");
        MvcResult missing = requestReset("missing@example.org", "192.0.2.2");

        assertThat(existing.getResponse().getStatus()).isEqualTo(202);
        assertThat(missing.getResponse().getStatus()).isEqualTo(202);
        assertThat(existing.getResponse().getContentAsString())
                .isEqualTo(missing.getResponse().getContentAsString());

        JsonNode response = new ObjectMapper().readTree(existing.getResponse().getContentAsString());
        assertThat(response.has("token")).isFalse();
        assertThat(response.fieldNames()).toIterable().containsExactly("message");
        assertThat(sentEmails).hasSize(1);

        String rawToken = lastEmailedToken();
        assertThat(Base64.getUrlDecoder().decode(rawToken)).hasSize(32);
        ConfirmationToken stored = tokenRepository.findAll().getFirst();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(stored.toString()).doesNotContain(rawToken);
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        User user = createUser("expired@example.org");
        String rawToken = "expired-token";
        storeToken(user, rawToken, ConfirmationTokenType.PASSWORD_RESET,
                Instant.now().minusSeconds(60), null);

        confirm(rawToken, STRONG_PASSWORD).andExpect(status().isBadRequest());
        assertThat(passwordEncoder.matches(STRONG_PASSWORD,
                userRepository.findById(user.getId()).orElseThrow().getPassword())).isFalse();
    }

    @Test
    void rejectsTheWrongTokenType() throws Exception {
        User user = createUser("type@example.org");
        String rawToken = "email-confirmation-token";
        storeToken(user, rawToken, ConfirmationTokenType.EMAIL_CONFIRMATION,
                Instant.now().plusSeconds(600), null);

        confirm(rawToken, STRONG_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void tokenCannotBeReused() throws Exception {
        createUser("reuse@example.org");
        requestReset("reuse@example.org", "192.0.2.3");
        String rawToken = lastEmailedToken();

        confirm(rawToken, STRONG_PASSWORD).andExpect(status().isOk());
        confirm(rawToken, "Encore!Secret43").andExpect(status().isBadRequest());
    }

    @Test
    void onlyOneConcurrentConfirmationCanConsumeTheToken() throws Exception {
        User user = createUser("race@example.org");
        requestReset("race@example.org", "192.0.2.4");
        String rawToken = lastEmailedToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> concurrentConfirm(rawToken, "Premier!Secret42", ready, start));
            Future<Integer> second = executor.submit(() -> concurrentConfirm(rawToken, "Second!Secret42", ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 400);
        }
        assertThat(userRepository.findById(user.getId()).orElseThrow().getSessionVersion()).isEqualTo(1);
    }

    @Test
    void aNewRequestInvalidatesAllOlderResetTokens() throws Exception {
        createUser("old@example.org");
        requestReset("old@example.org", "192.0.2.5");
        String oldToken = lastEmailedToken();
        requestReset("old@example.org", "192.0.2.6");
        String currentToken = lastEmailedToken();

        confirm(oldToken, STRONG_PASSWORD).andExpect(status().isBadRequest());
        confirm(currentToken, STRONG_PASSWORD).andExpect(status().isOk());
        assertThat(tokenRepository.findAll()).allSatisfy(token -> assertThat(token.getUsedAt()).isNotNull());
    }

    @Test
    void weakPasswordIsRejectedWithoutConsumingTheToken() throws Exception {
        createUser("policy@example.org");
        requestReset("policy@example.org", "192.0.2.7");
        String rawToken = lastEmailedToken();

        confirm(rawToken, "password")
                .andExpect(status().isBadRequest());
        confirm(rawToken, STRONG_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Réinitialisation du mot de passe réussie"));
    }

    private MvcResult requestReset(String username, String remoteAddress) throws Exception {
        return mockMvc.perform(post("/auth/reinitPassword")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
        String body = new ObjectMapper().writeValueAsString(new ConfirmBody(token, password));
        return mockMvc.perform(post("/auth/changePassword")
                .contentType("application/json")
                .content(body));
    }

    private int concurrentConfirm(String token, String password, CountDownLatch ready,
                                  CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return confirm(token, password).andReturn().getResponse().getStatus();
    }

    private User createUser(String username) {
        return userRepository.saveAndFlush(new User(username, passwordEncoder.encode("Ancien!Secret42")));
    }

    private void storeToken(User user, String rawToken, ConfirmationTokenType type,
                            Instant expiresAt, Instant usedAt) {
        ConfirmationToken token = new ConfirmationToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(ConfirmationTokenService.hash(rawToken));
        token.setCreatedAt(Instant.now().minusSeconds(120));
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        tokenRepository.saveAndFlush(token);
    }

    private String lastEmailedToken() {
        Matcher matcher = TOKEN_IN_EMAIL.matcher(sentEmails.getLast().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private record ConfirmBody(String token, String password) {
    }

    @Configuration
    @EnableWebMvc
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, ConfirmationTokenRepository.class})
    @Import({AuthController.class, PasswordResetService.class,
            PasswordResetWorker.class, ConfirmationTokenService.class, PasswordResetRateLimiter.class})
    static class TestConfiguration {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(true);
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("com.wild.corp.adhesion.models");
            Properties properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
