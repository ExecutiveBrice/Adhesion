package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Chat;
import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.resources.ChatConfiguration;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import com.wild.corp.adhesion.repository.ChatRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.ChatConfigurationServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SectionManagementControllerTest {
    private final Principal referent = () -> "referent@example.org";
    private SectionRepository sections;
    private ActiviteRepository activites;
    private AdhesionRepository adhesions;
    private ChatRepository chats;
    private ChatConfigurationServices chatConfigurations;
    private AdhesionServices adhesionServices;
    private ActiviteServices activiteServices;
    private SectionManagementController controller;

    @BeforeEach
    void setUp() {
        sections = mock(SectionRepository.class);
        activites = mock(ActiviteRepository.class);
        adhesions = mock(AdhesionRepository.class);
        chats = mock(ChatRepository.class);
        chatConfigurations = mock(ChatConfigurationServices.class);
        adhesionServices = mock(AdhesionServices.class);
        activiteServices = mock(ActiviteServices.class);
        controller = new SectionManagementController(sections, activites, adhesions, chats,
                chatConfigurations, adhesionServices, activiteServices);
    }

    @Test
    void listsOnlyAssignedSections() {
        Section section = managedSection();
        when(sections.findManagedByUsername(referent.getName())).thenReturn(List.of(section));

        assertThat(controller.managedSections(referent)).containsExactly(
                new SectionManagementController.ManagedSection(4L, "Basket", Section.COULEUR_PAR_DEFAUT));
    }

    @Test
    void deniesASectionWithoutAnAssignment() {
        when(sections.findById(4L)).thenReturn(Optional.of(Section.builder().id(4L).nom("Basket").build()));

        assertThatThrownBy(() -> controller.activities(referent, 4L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(activites);
    }

    @Test
    void cannotModifyAnActivityInAnotherSection() {
        when(sections.findById(4L)).thenReturn(Optional.of(managedSection()));
        Activite activity = new Activite();
        activity.setId(10L);
        activity.setSection(Section.builder().id(5L).build());
        when(activites.findById(10L)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> controller.updateActivity(referent, 4L, 10L,
                new SectionManagementController.ActivityChanges("Autre", 100, 20, false, false, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(activites, never()).save(any());
    }

    @Test
    void cannotMoveAChatOutsideItsSectionOrEditAnOutsideChat() {
        when(sections.findById(4L)).thenReturn(Optional.of(managedSection()));
        Chat chat = new Chat();
        chat.setId(20L);
        chat.setCible(ChatTarget.SECTION);
        chat.setSection(Section.builder().id(4L).build());
        when(chats.findById(20L)).thenReturn(Optional.of(chat));
        Activite outside = new Activite();
        outside.setId(30L);
        outside.setSection(Section.builder().id(5L).build());
        when(activites.findById(30L)).thenReturn(Optional.of(outside));

        ChatConfiguration moved = new ChatConfiguration(20L, "Informations", "ACTIVITE", 30L, null, List.of());
        assertThatThrownBy(() -> controller.updateChat(referent, 4L, 20L, moved))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verify(chatConfigurations, never()).update(anyLong(), anyLong(), any());

        chat.setSection(Section.builder().id(5L).build());
        assertThatThrownBy(() -> controller.deleteChat(referent, 4L, 20L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(chatConfigurations, never()).delete(anyLong(), anyLong());
    }

    @Test
    void listsOnlySectionChatsForAnAssignedReferent() {
        when(sections.findById(4L)).thenReturn(Optional.of(managedSection()));
        ChatConfiguration sectionChat = new ChatConfiguration(20L, "Informations", "SECTION", 4L, "Basket", List.of());
        when(chatConfigurations.getAll(4L)).thenReturn(List.of(sectionChat));

        assertThat(controller.chats(referent, 4L)).containsExactly(sectionChat);
        verify(chatConfigurations).getAll(4L);
        verify(chatConfigurations, never()).getAll();
    }

    @Test
    void cannotListChatsOfAnUnassignedSection() {
        when(sections.findById(5L)).thenReturn(Optional.of(Section.builder().id(5L).nom("Autre").build()));

        assertThatThrownBy(() -> controller.chats(referent, 5L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(chatConfigurations);
    }

    @Test
    void cannotCreateAChatForAnotherTarget() {
        when(sections.findById(4L)).thenReturn(Optional.of(managedSection()));

        ChatConfiguration global = new ChatConfiguration(null, "Informations", "ASSOCIATION", null, null, List.of());
        ChatConfiguration outside = new ChatConfiguration(null, "Informations", "SECTION", 5L, null, List.of());
        assertThatThrownBy(() -> controller.createChat(referent, 4L, global))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> controller.createChat(referent, 4L, outside))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verify(chatConfigurations, never()).create(any());
    }

    @Test
    void cannotChangeTheStatusOfAnOutsideMembership() {
        when(sections.findById(4L)).thenReturn(Optional.of(managedSection()));
        Activite activity = new Activite();
        activity.setSection(Section.builder().id(5L).build());
        Adhesion membership = new Adhesion();
        membership.setActivite(activity);
        when(adhesions.findById(50L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> controller.updateMembershipStatus(referent, 4L, 50L,
                new SectionManagementController.StatusChange("Validée")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(adhesionServices);
    }

    private Section managedSection() {
        Section section = Section.builder().id(4L).nom("Basket").build();
        User user = new User(referent.getName(), "password");
        section.getReferents().add(user);
        return section;
    }
}
