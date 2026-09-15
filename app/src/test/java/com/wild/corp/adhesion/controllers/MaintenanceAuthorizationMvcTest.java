package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.services.UserServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdherentController.class)
@ContextConfiguration(classes = {AdherentController.class, MaintenanceAuthorizationMvcTest.SecurityTestConfiguration.class})
class MaintenanceAuthorizationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdherentServices adherentServices;
    @MockitoBean
    private UserServices userServices;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @Test
    void anonymousUserIsRejected() throws Exception {
        mockMvc.perform(post("/adherent/nouvelleAnnee"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserIsRejectedFromEveryMaintenanceOperation() throws Exception {
        var user = user("member@example.org").roles("USER");

        mockMvc.perform(post("/adherent/nouvelleAnnee").with(user)).andExpect(status().isForbidden());
        mockMvc.perform(post("/adherent/cleanNotification").with(user)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/adherent/cleanUserAlone").with(user)).andExpect(status().isForbidden());
        mockMvc.perform(post("/adherent/regenerate").param("adherentId", "1").with(user)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanRunEveryMaintenanceOperation() throws Exception {
        var admin = user("admin@example.org").roles("ADMIN");

        mockMvc.perform(post("/adherent/nouvelleAnnee").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/adherent/cleanNotification").with(admin)).andExpect(status().isOk());
        mockMvc.perform(delete("/adherent/cleanUserAlone").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/adherent/regenerate").param("adherentId", "1").with(admin)).andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
