package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Chat;
import com.wild.corp.adhesion.models.resources.ChatConfiguration;
import com.wild.corp.adhesion.repository.ChatRepository;
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

class ChatConfigurationServicesTest {

    private SectionRepository sections;
    private ActiviteRepository activites;
    private ChatRepository chats;
    private ChatConfigurationServices service;

    @BeforeEach
    void setUp() {
        sections = mock(SectionRepository.class);
        activites = mock(ActiviteRepository.class);
        chats = mock(ChatRepository.class);
        service = new ChatConfigurationServices(sections, activites, chats);
    }

    @Test
    void createsAChatWithReadAndWritePermissions() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        when(sections.findById(4L)).thenReturn(Optional.of(section));
        when(chats.findAll()).thenReturn(List.of());
        when(chats.save(any())).thenAnswer(invocation -> {
            Chat chat = invocation.getArgument(0);
            chat.setId(8L);
            return chat;
        });

        var created = service.create(new ChatConfiguration(null, "  Encadrants  ", "SECTION", 4L, null, List.of(
                new ChatConfiguration.RolePermission("role_encadrant", true),
                new ChatConfiguration.RolePermission("ROLE_REFERENT_ACTIVITE", false))));

        assertThat(created).isEqualTo(new ChatConfiguration(8L, "Encadrants", "SECTION", 4L, "Section · Basket", List.of(
                new ChatConfiguration.RolePermission("ROLE_ENCADRANT", true),
                new ChatConfiguration.RolePermission("ROLE_REFERENT_ACTIVITE", false))));
    }

    @Test
    void allowsLinkedChatsWithoutRolePermissions() {
        when(sections.findById(4L)).thenReturn(Optional.of(Section.builder().id(4L).nom("Basket").type("COMPETITION").build()));
        when(chats.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(new ChatConfiguration(null, "Encadrants", "SECTION", 4L, null, List.of())).permissions())
                .isEmpty();
    }

    @Test
    void requiresAtLeastOneRoleForGlobalChatsAndRejectsConflictingTarget() {
        assertThatThrownBy(() -> service.create(new ChatConfiguration(null, "Global", "ASSOCIATION", null, null, List.of())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("au moins un rôle");
        assertThatThrownBy(() -> service.create(new ChatConfiguration(null, "Global", "ASSOCIATION", 4L, null,
                List.of(new ChatConfiguration.RolePermission("ROLE_ADMIN", true)))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("global");
    }

    @Test
    void rejectsDuplicateAndUnknownRolePermissions() {
        for (var permissions : List.of(
                List.of(new ChatConfiguration.RolePermission("ROLE_UNKNOWN", false)),
                List.of(new ChatConfiguration.RolePermission("ROLE_ADMIN", true),
                        new ChatConfiguration.RolePermission("role_admin", false)))) {
            assertThatThrownBy(() -> service.create(new ChatConfiguration(null, "Global", "ASSOCIATION", null, null, permissions)))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void createsAChatAttachedToAnActivity() {
        Activite activite = new Activite();
        activite.setId(12L);
        activite.setNom("Yoga");
        when(activites.findById(12L)).thenReturn(Optional.of(activite));
        when(chats.findAll()).thenReturn(List.of());
        when(chats.save(any())).thenAnswer(invocation -> {
            Chat chat = invocation.getArgument(0);
            chat.setId(9L);
            return chat;
        });

        var created = service.create(new ChatConfiguration(null, "Informations", "ACTIVITE", 12L, null, List.of(
                new ChatConfiguration.RolePermission("ROLE_USER", false))));

        assertThat(created.cible()).isEqualTo("ACTIVITE");
        assertThat(created.cibleNom()).isEqualTo("Activité · Yoga");
    }

    @Test
    void updatesTheNameAndPermissionsOfAChat() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        Chat chat = new Chat();
        chat.setId(8L);
        chat.setSection(section);
        chat.setCible(com.wild.corp.adhesion.models.ChatTarget.SECTION);
        chat.setNom("Encadrants");
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(sections.findById(4L)).thenReturn(Optional.of(section));
        when(chats.findAll()).thenReturn(List.of(chat));
        when(chats.save(chat)).thenReturn(chat);

        var updated = service.update(8L, new ChatConfiguration(null, " Informations ", "SECTION", 4L, null, List.of(
                new ChatConfiguration.RolePermission("ROLE_USER", false),
                new ChatConfiguration.RolePermission("ROLE_ADMIN", true))));

        assertThat(updated).isEqualTo(new ChatConfiguration(8L, "Informations", "SECTION", 4L, "Section · Basket", List.of(
                new ChatConfiguration.RolePermission("ROLE_USER", false),
                new ChatConfiguration.RolePermission("ROLE_ADMIN", true))));
    }

    @Test
    void deletesOnlyAChatFromItsSection() {
        Section section = Section.builder().id(4L).nom("Basket").type("COMPETITION").build();
        Chat chat = new Chat();
        chat.setId(8L);
        chat.setSection(section);
        chat.setCible(com.wild.corp.adhesion.models.ChatTarget.SECTION);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));

        service.delete(4L, 8L);

        verify(chats).delete(chat);
    }

    @Test
    void changingTargetPreservesExistingPermissionEntities() {
        Chat chat = new Chat();
        chat.setId(8L); chat.setNom("Informations");
        chat.setCible(com.wild.corp.adhesion.models.ChatTarget.ASSOCIATION);
        var permission = new com.wild.corp.adhesion.models.ChatPermission();
        permission.setId(40L); permission.setChat(chat); permission.setRole("ROLE_ADMIN");
        chat.getPermissions().add(permission);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAll()).thenReturn(List.of(chat));
        when(chats.save(chat)).thenReturn(chat);
        when(sections.findById(4L)).thenReturn(Optional.of(Section.builder().id(4L).nom("Basket").build()));
        service.update(8L, new ChatConfiguration(8L, "Informations", "SECTION", 4L, null,
                List.of(new ChatConfiguration.RolePermission("ROLE_ADMIN", true))));
        assertThat(chat.getPermissions()).containsExactly(permission);
        assertThat(permission.getId()).isEqualTo(40L);
        assertThat(permission.isEcriture()).isTrue();
        assertThat(chat.getActivite()).isNull();
        service.update(8L, new ChatConfiguration(8L, "Informations", "ASSOCIATION", null, null,
                List.of(new ChatConfiguration.RolePermission("ROLE_ADMIN", false))));
        assertThat(chat.getSection()).isNull();
        assertThat(chat.getPermissions()).containsExactly(permission);
    }
}
