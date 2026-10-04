package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.SectionChat;
import com.wild.corp.adhesion.models.resources.SectionChatConfiguration;
import com.wild.corp.adhesion.repository.SectionChatRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionChatConfigurationServicesTest {

    private SectionRepository sections;
    private ActiviteRepository activites;
    private SectionChatRepository chats;
    private SectionChatConfigurationServices service;

    @BeforeEach
    void setUp() {
        sections = mock(SectionRepository.class);
        activites = mock(ActiviteRepository.class);
        chats = mock(SectionChatRepository.class);
        service = new SectionChatConfigurationServices(sections, activites, chats);
    }

    @Test
    void createsAChatWithReadAndWritePermissions() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        when(sections.findById(4L)).thenReturn(Optional.of(section));
        when(chats.findAll()).thenReturn(List.of());
        when(chats.save(any())).thenAnswer(invocation -> {
            SectionChat chat = invocation.getArgument(0);
            chat.setId(8L);
            return chat;
        });

        var created = service.create(new SectionChatConfiguration(null, "  Encadrants  ", "SECTION", 4L, null, List.of(
                new SectionChatConfiguration.RolePermission("role_encadrant", true),
                new SectionChatConfiguration.RolePermission("ROLE_REFERENT_ACTIVITE", false))));

        assertThat(created).isEqualTo(new SectionChatConfiguration(8L, "Encadrants", "SECTION", 4L, "Section · Basket", List.of(
                new SectionChatConfiguration.RolePermission("ROLE_ENCADRANT", true),
                new SectionChatConfiguration.RolePermission("ROLE_REFERENT_ACTIVITE", false))));
    }

    @Test
    void requiresAtLeastOneReadableRole() {
        when(sections.findById(4L)).thenReturn(Optional.of(Section.builder().id(4L).nom("Basket").type("COMPETITION").build()));
        assertThatThrownBy(() -> service.create(new SectionChatConfiguration(null, "Encadrants", "SECTION", 4L, null, List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("au moins un rôle");
    }

    @Test
    void createsAChatAttachedToAnActivity() {
        Activite activite = new Activite();
        activite.setId(12L);
        activite.setNom("Yoga");
        when(activites.findById(12L)).thenReturn(Optional.of(activite));
        when(chats.findAll()).thenReturn(List.of());
        when(chats.save(any())).thenAnswer(invocation -> {
            SectionChat chat = invocation.getArgument(0);
            chat.setId(9L);
            return chat;
        });

        var created = service.create(new SectionChatConfiguration(null, "Informations", "ACTIVITE", 12L, null, List.of(
                new SectionChatConfiguration.RolePermission("ROLE_USER", false))));

        assertThat(created.cible()).isEqualTo("ACTIVITE");
        assertThat(created.cibleNom()).isEqualTo("Activité · Yoga");
    }

    @Test
    void updatesTheNameAndPermissionsOfAChat() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        SectionChat chat = new SectionChat();
        chat.setId(8L);
        chat.setSection(section);
        chat.setCible(com.wild.corp.adhesion.models.ChatTarget.SECTION);
        chat.setNom("Encadrants");
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(sections.findById(4L)).thenReturn(Optional.of(section));
        when(chats.findAll()).thenReturn(List.of(chat));
        when(chats.save(chat)).thenReturn(chat);

        var updated = service.update(8L, new SectionChatConfiguration(null, " Informations ", "SECTION", 4L, null, List.of(
                new SectionChatConfiguration.RolePermission("ROLE_USER", false),
                new SectionChatConfiguration.RolePermission("ROLE_ADMIN", true))));

        assertThat(updated).isEqualTo(new SectionChatConfiguration(8L, "Informations", "SECTION", 4L, "Section · Basket", List.of(
                new SectionChatConfiguration.RolePermission("ROLE_USER", false),
                new SectionChatConfiguration.RolePermission("ROLE_ADMIN", true))));
    }

    @Test
    void deletesOnlyAChatFromItsSection() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        SectionChat chat = new SectionChat();
        chat.setId(8L);
        chat.setSection(section);
        chat.setCible(com.wild.corp.adhesion.models.ChatTarget.SECTION);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));

        service.delete(4L, 8L);

        verify(chats).delete(chat);
    }
}
