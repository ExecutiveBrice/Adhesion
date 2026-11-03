package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.MaterielServices;
import com.wild.corp.adhesion.models.resources.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MaterielController.class)
@ContextConfiguration(classes = {MaterielController.class, MaterielControllerTest.SecurityTestConfiguration.class})
class MaterielControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private MaterielServices service;

    @Test
    void anonymousAndOrdinaryMembersCannotAccessMaterialOrZones() throws Exception {
        mvc.perform(get("/materiel")).andExpect(status().isUnauthorized());
        for (String role : new String[] {"USER", "BUREAU", "SECRETAIRE", "ENCADRANT", "COMPTABLE"}) {
            mvc.perform(get("/materiel").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(get("/materiel/zonages").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post("/materiel/2/emprunt").with(user("member").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post("/materiel/2/retour").with(user("member").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void borrowingUsesTheAuthenticatedActorAndRole() throws Exception {
        for (String role : new String[] {"MEMBRECA", "ADMIN"}) {
            mvc.perform(get("/materiel").with(user("connected").roles(role))).andExpect(status().isOk());
            mvc.perform(get("/materiel/zonages").with(user("connected").roles(role))).andExpect(status().isOk());
            mvc.perform(post("/materiel/2/emprunt").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"someone-else\"}")
                    .with(user("connected").roles(role))).andExpect(status().isOk());
        }
        verify(service).emprunter(2L, "connected", null, false, 1);
        verify(service).emprunter(2L, "connected", null, true, 1);
    }

    @Test
    void caCannotCreateMaterialOrConfigureZones() throws Exception {
        mvc.perform(post("/materiel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Objet\",\"zonageId\":1}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/materiel/zonages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Zone\",\"couleur\":\"#123456\"}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/materiel/zonages/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Zone\",\"couleur\":\"#123456\"}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/materiel/zonages/1").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void adminCanCreateAndConfigure() throws Exception {
        mvc.perform(post("/materiel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Objet\",\"zonageId\":1}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());
        mvc.perform(post("/materiel/zonages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Zone\",\"couleur\":\"#123456\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());
        mvc.perform(put("/materiel/zonages/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Zone\",\"couleur\":\"#123456\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(delete("/materiel/zonages/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(service).create(new MaterielRequest("Objet", 1L));
        verify(service).createZonage(new ZonageConfiguration(null, "Zone", "#123456"));
        verify(service).updateZonage(1L, new ZonageConfiguration(null, "Zone", "#123456"));
        verify(service).deleteZonage(1L);
    }

    @Test
    void returnsUseAuthenticatedIdentityAndServerGrantedAdminRole() throws Exception {
        mvc.perform(post("/materiel/2/retour").with(user("ca").roles("MEMBRECA"))).andExpect(status().isOk());
        mvc.perform(post("/materiel/2/retour").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(service).restituer(2L, "ca", false, null);
        verify(service).restituer(2L, "admin", true, null);
    }

    @Test
    void conflictIncludesAUsefulMessageForTheUi() throws Exception {
        when(service.emprunter(2L, "ca", null, false, 1))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ce matériel est déjà emprunté"));
        mvc.perform(post("/materiel/2/emprunt").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Ce matériel est déjà emprunté"));
    }

    @Test
    void onlyAdminsCanChooseACaMemberOrChangeHsAndCaCanReadHistory() throws Exception {
        mvc.perform(get("/materiel/membres-ca").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/materiel/2/hors-service").contentType(MediaType.APPLICATION_JSON)
                .content("{\"horsService\":true,\"commentaire\":\"Lampe cassée\"}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/materiel/2/historique").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/materiel/2/historique").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isOk());
        mvc.perform(get("/materiel/membres-ca").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(patch("/materiel/2/hors-service").contentType(MediaType.APPLICATION_JSON)
                .content("{\"horsService\":true,\"commentaire\":\"Lampe cassée\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(post("/materiel/2/emprunt").contentType(MediaType.APPLICATION_JSON)
                .content("{\"emprunteurId\":4}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service).getHistorique(2L);
        verify(service).getMembresCA();
        verify(service).updateHorsService(2L, new HorsServiceRequest(true, "Lampe cassée"), "admin");
        verify(service).emprunter(2L, "admin", 4L, true, 1);
    }

    @Test
    void quantitiesAndSelectedLoanAreForwardedToTheService() throws Exception {
        mvc.perform(post("/materiel/2/emprunt").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantite\":3}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isOk());
        verify(service).emprunter(2L, "ca", null, false, 3);
        mvc.perform(post("/materiel/2/retour").contentType(MediaType.APPLICATION_JSON)
                .content("{\"empruntId\":15,\"quantite\":2}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isOk());
        verify(service).restituer(2L, "ca", false, new RetourMaterielRequest(15L, 2));
    }

    @Test
    void selectedBorrowerCannotForgeTheAdminRole() throws Exception {
        when(service.emprunter(2L, "ca", 4L, false, 1))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul un administrateur peut choisir l’emprunteur"));
        mvc.perform(post("/materiel/2/emprunt").contentType(MediaType.APPLICATION_JSON)
                .content("{\"emprunteurId\":4,\"administrateur\":true}").with(user("ca").roles("MEMBRECA")))
                .andExpect(status().isForbidden());
        verify(service).emprunter(2L, "ca", 4L, false, 1);
    }

    @Test
    void logisticsCanManageMaterialAndZonesButCannotManageLoans() throws Exception {
        mvc.perform(get("/materiel").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isOk());
        mvc.perform(get("/materiel/zonages").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isOk());
        mvc.perform(get("/materiel/2/historique").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isOk());
        mvc.perform(post("/materiel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Objet\",\"zonageId\":1}").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isCreated());
        mvc.perform(put("/materiel/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Nouveau nom\",\"zonageId\":3}").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isOk());
        mvc.perform(delete("/materiel/2").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isNoContent());
        mvc.perform(post("/materiel/2/emprunt").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isForbidden());
        mvc.perform(post("/materiel/2/retour").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isForbidden());
        mvc.perform(patch("/materiel/2/hors-service").contentType(MediaType.APPLICATION_JSON)
                .content("{\"horsService\":true,\"commentaire\":\"Lampe cassée\"}").with(user("logistique").roles("LOGISTIQUE"))).andExpect(status().isForbidden());
        mvc.perform(post("/materiel/zonages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Zone\",\"couleur\":\"#123456\"}").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isCreated());
        mvc.perform(put("/materiel/zonages/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Réserve\",\"couleur\":\"#654321\"}").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isOk());
        mvc.perform(delete("/materiel/zonages/1").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isNoContent());
        verify(service).create(new MaterielRequest("Objet", 1L));
        verify(service).update(2L, new MaterielRequest("Nouveau nom", 3L));
        verify(service).delete(2L);
        verify(service).createZonage(new ZonageConfiguration(null, "Zone", "#123456"));
        verify(service).updateZonage(1L, new ZonageConfiguration(null, "Réserve", "#654321"));
        verify(service).deleteZonage(1L);
        verify(service, never()).emprunter(any(), any(), any(), anyBoolean());
    }

    @Test
    void logisticsCannotDeleteAZoneContainingMaterial() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ce zonage contient du matériel et ne peut pas être supprimé"))
                .when(service).deleteZonage(1L);
        mvc.perform(delete("/materiel/zonages/1").with(user("logistique").roles("LOGISTIQUE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ce zonage contient du matériel et ne peut pas être supprimé"));
    }

    @Test
    void onlyAdminAndLogisticsCanEditOrDelete() throws Exception {
        for (String role : new String[] {"USER", "MEMBRECA", "BUREAU", "SECRETAIRE", "ENCADRANT", "COMPTABLE"}) {
            mvc.perform(put("/materiel/2").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nom\":\"Objet\",\"zonageId\":1}").with(user("member").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/materiel/2").with(user("member").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
        mvc.perform(put("/materiel/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Objet\",\"zonageId\":1}").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(delete("/materiel/2").with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }
    }
}
