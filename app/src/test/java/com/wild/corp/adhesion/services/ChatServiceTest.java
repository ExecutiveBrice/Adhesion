package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.SectionChat;
import com.wild.corp.adhesion.models.SectionChatPermission;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.ChatMessageRepository;
import com.wild.corp.adhesion.repository.ChatReadRepository;
import com.wild.corp.adhesion.repository.SectionChatRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActiviteRepository activities = mock(ActiviteRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatReadRepository reads = mock(ChatReadRepository.class);
    private final SectionChatRepository chats = mock(SectionChatRepository.class);
    private final ChatService service = new ChatService(users, activities, messages, reads, chats);
    private final User user = new User("member@example.test", "password");

    @BeforeEach
    void setUp() {
        user.setId(4L);
        user.setRoles(java.util.Set.of(ERole.ROLE_USER));
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    @Test
    void listsOnlyChatsCreatedFromAdministration() {
        SectionChat chat = associationChat(8L, "Informations");
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(reads.sectionUnreadCounts(4L, List.of(8L))).thenReturn(java.util.Map.of(8L, 2L));

        var rooms = service.rooms(user.getUsername());

        assertThat(rooms).extracting(ChatService.Room::nom).containsExactly("Informations");
        assertThat(rooms.getFirst().sectionChatId()).isEqualTo(8L);
        assertThat(rooms.getFirst().unreadCount()).isEqualTo(2L);
    }

    @Test
    void doesNotExposeChatsWithoutAConfiguredPermission() {
        SectionChat chat = associationChat(8L, "Informations");
        chat.getPermissions().clear();
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(reads.sectionUnreadCounts(4L, List.of())).thenReturn(java.util.Map.of());

        assertThat(service.rooms(user.getUsername())).isEmpty();
    }

    @Test
    void rejectsLegacyActivityRooms() {
        assertThatThrownBy(() -> service.history(user.getUsername(), 7L, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.send(user.getUsername(), ChatService.COMMUNICATION_ROOM_ID, "Bonjour"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.NOT_FOUND);
    }

    private SectionChat associationChat(Long id, String nom) {
        SectionChat chat = new SectionChat();
        chat.setId(id);
        chat.setNom(nom);
        chat.setCible(ChatTarget.ASSOCIATION);
        SectionChatPermission permission = new SectionChatPermission();
        permission.setChat(chat);
        permission.setRole("ROLE_USER");
        permission.setEcriture(true);
        chat.getPermissions().add(permission);
        return chat;
    }
}
