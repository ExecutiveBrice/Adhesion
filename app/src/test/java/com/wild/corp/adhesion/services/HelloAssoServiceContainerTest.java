package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.config.HelloAssoProperties;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.helloasso.HelloAssoCheckoutRequest;
import com.wild.corp.adhesion.models.helloasso.HelloAssoCheckoutResponse;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import com.wild.corp.adhesion.services.helloasso.HelloAssoAuthClient;
import com.wild.corp.adhesion.services.helloasso.HelloAssoCheckoutClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HelloAssoServiceContainerTest.TestConfig.class)
@Testcontainers
class HelloAssoServiceContainerTest {

    @Container
    static MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String endpoint = "http://" + mockServerContainer.getHost() + ":" + mockServerContainer.getServerPort();
        registry.add("helloasso.base-url", () -> endpoint);
        registry.add("helloasso.oauth-url", () -> endpoint + "/oauth2/token");
        registry.add("helloasso.organization-slug", () -> "asso-test");
        registry.add("helloasso.client-id", () -> "client-id-test");
        registry.add("helloasso.client-secret", () -> "client-secret-test");
    }

    @Autowired
    private HelloAssoService helloAssoService;

    @MockBean
    private AdhesionRepository adhesionRepository;

    private MockServerClient mockServerClient;

    @BeforeEach
    void setUp() {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
        mockServerClient.reset();
    }

    @Test
    void shouldCreateCheckoutIntent() {
        Adhesion adhesion = new Adhesion();
        adhesion.setId(42L);
        adhesion.setTarif(180);
        Activite activite = new Activite();
        activite.setNom("Basket U13");
        adhesion.setActivite(activite);

        when(adhesionRepository.findById(42L)).thenReturn(Optional.of(adhesion));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/oauth2/token"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"access_token\":\"token-123\",\"expires_in\":3600,\"token_type\":\"Bearer\"}"));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/organizations/asso-test/checkout-intents"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"id\":12345,\"redirectUrl\":\"https://helloasso.test/checkout/12345\"}"));

        HelloAssoCheckoutRequest request = new HelloAssoCheckoutRequest();
        request.setAdhesionId(42L);
        request.setBackUrl("https://front.test/back");
        request.setErrorUrl("https://front.test/error");
        request.setReturnUrl("https://front.test/return");

        HelloAssoCheckoutResponse response = helloAssoService.createCheckoutIntent(request);

        assertThat(response.getCheckoutIntentId()).isEqualTo(12345);
        assertThat(response.getRedirectUrl()).isEqualTo("https://helloasso.test/checkout/12345");

        mockServerClient.verify(request().withMethod("POST").withPath("/oauth2/token"));
        mockServerClient.verify(request().withMethod("POST").withPath("/organizations/asso-test/checkout-intents"));
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(HelloAssoProperties.class)
    @EnableFeignClients(clients = {HelloAssoAuthClient.class, HelloAssoCheckoutClient.class})
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        HelloAssoService helloAssoService(HelloAssoProperties properties,
                                          AdhesionRepository adhesionRepository,
                                          HelloAssoAuthClient helloAssoAuthClient,
                                          HelloAssoCheckoutClient helloAssoCheckoutClient) {
            return new HelloAssoService(properties, adhesionRepository, helloAssoAuthClient, helloAssoCheckoutClient);
        }
    }

}
