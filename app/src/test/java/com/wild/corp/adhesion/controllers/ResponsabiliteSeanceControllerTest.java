package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.models.resources.ResponsabiliteSeanceRequest;
import com.wild.corp.adhesion.models.resources.TacheSeanceConfiguration;
import com.wild.corp.adhesion.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ParamController.class, ActiviteController.class})
@ContextConfiguration(classes = {ParamController.class, ActiviteController.class,
        SeanceControllerTest.SecurityTestConfiguration.class})
class ResponsabiliteSeanceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ParamBooleanServices paramBooleanServices;
    @MockitoBean ParamTextServices paramTextServices;
    @MockitoBean ParamNumberServices paramNumberServices;
    @MockitoBean GoogleAgendaConfigurationServices googleAgendaConfigurationServices;
    @MockitoBean SalleConfigurationServices salleConfigurationServices;
    @MockitoBean SectionConfigurationServices sectionConfigurationServices;
    @MockitoBean ChatConfigurationServices chatConfigurationServices;
    @MockitoBean TacheSeanceConfigurationServices tacheSeanceConfigurationServices;
    @MockitoBean ActiviteServices activiteServices;
    @MockitoBean SeanceServices seanceServices;
    @MockitoBean GoogleAgendaServices googleAgendaServices;
    @MockitoBean ResponsabiliteSeanceServices responsabiliteSeanceServices;

    @Test
    void onlyAdministratorCanManageTaskCatalogWhileMembersCanReadIt() throws Exception {
        when(tacheSeanceConfigurationServices.getAll()).thenReturn(List.of(new TacheSeanceConfiguration(3L, "Matériel")));
        mvc.perform(get("/param/taches-seance").with(user("member").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nom").value("Matériel"));
        mvc.perform(post("/param/taches-seance").with(user("member").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Matériel\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/param/taches-seance/3").with(user("member").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Accueil\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/param/taches-seance/3").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        var configuration = new TacheSeanceConfiguration(null, "Matériel");
        when(tacheSeanceConfigurationServices.create(configuration)).thenReturn(new TacheSeanceConfiguration(3L, "Matériel"));
        mvc.perform(post("/param/taches-seance").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Matériel\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        verify(tacheSeanceConfigurationServices).create(configuration);
    }

    @Test
    void rejectsIncompleteAssignmentsBeforeCallingTheSessionService() throws Exception {
        for (String responsabilite : List.of("null", "{}", "{\"tacheId\":3}", "{\"tacheId\":0,\"adherentId\":12}")) {
            mvc.perform(patch("/activite/5/seances/9").with(user("secretary").roles("SECRETAIRE"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"responsabilites\":[" + responsabilite + "]}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(seanceServices);
    }

    @Test
    void administratorCanSaveTaskPresentation() throws Exception {
        var configuration = new TacheSeanceConfiguration(null, "Accueil", "handshake", "#AB47BC");
        when(tacheSeanceConfigurationServices.create(configuration))
                .thenReturn(new TacheSeanceConfiguration(3L, "Accueil", "handshake", "#AB47BC"));
        mvc.perform(post("/param/taches-seance").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Accueil\",\"icone\":\"handshake\",\"couleur\":\"#AB47BC\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.icone").value("handshake"))
                .andExpect(jsonPath("$.couleur").value("#AB47BC"));
        verify(tacheSeanceConfigurationServices).create(configuration);
    }

    @Test
    void patchesAssignmentsAndAcceptsAnEmptyListToRemoveThem() throws Exception {
        var responsabilites = List.of(new ResponsabiliteSeanceRequest(3L, 12L));
        when(seanceServices.updateSeance(5L, 9L, null, null, false, null, null, false, null, false, responsabilites))
                .thenReturn(new Seance());
        mvc.perform(patch("/activite/5/seances/9").with(user("secretary").roles("SECRETAIRE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"responsabilites\":[{\"tacheId\":3,\"adherentId\":12}]}"))
                .andExpect(status().isOk());
        verify(seanceServices).updateSeance(5L, 9L, null, null, false, null, null, false, null, false, responsabilites);
        when(seanceServices.updateSeance(5L, 9L, null, null, false, null, null, false, null, false, List.of()))
                .thenReturn(new Seance());
        mvc.perform(patch("/activite/5/seances/9").with(user("secretary").roles("SECRETAIRE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"responsabilites\":[]}"))
                .andExpect(status().isOk());
        verify(seanceServices).updateSeance(5L, 9L, null, null, false, null, null, false, null, false, List.of());
    }

    @Test
    void ordinaryMembersCannotUseUnscopedSessionRoutes() throws Exception {
        mvc.perform(get("/activite/5/seances").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/activite/5/responsabilites/candidats").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/activite/5/seances/9").with(user("member").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"responsabilites\":[]}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/activite/5/seances/9").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(seanceServices, responsabiliteSeanceServices);
    }

    @Test
    void administratorCanReadSessionsAddedToAnActivity() throws Exception {
        when(activiteServices.getSeances(38L)).thenReturn(List.of(new SeanceResponse(
                3626L, ESeance.PROGRAMMEE, null,
                LocalDateTime.of(2026, 11, 24, 20, 45), LocalDateTime.of(2026, 11, 24, 22, 0),
                null, null, null, null)));

        mvc.perform(get("/activite/38/seances").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3626))
                .andExpect(jsonPath("$[0].debut").value("2026-11-24T20:45:00"));
        verify(activiteServices).getSeances(38L);
    }
}
