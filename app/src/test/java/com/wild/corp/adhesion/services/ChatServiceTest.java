package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActiviteRepository activities = mock(ActiviteRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatReadRepository reads = mock(ChatReadRepository.class);
    private final ChatRepository chats = mock(ChatRepository.class);
    private final ChatService service = new ChatService(users, activities, messages, reads, chats);
    private User user;

    @BeforeEach
    void setup() {
        user = new User("alice", "password");
        user.setId(4L);
        user.setRoles(Set.of(ERole.ROLE_USER));
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void linksGrantReadAccessToExactActivityAndItsSectionWithoutAnyRoleGrant() {
        Chat own = room(ChatTarget.ACTIVITE, 8L, 30L);
        Chat other = room(ChatTarget.ACTIVITE, 9L, 31L);
        Chat section = room(ChatTarget.SECTION, 10L, 20L);
        Chat global = room(ChatTarget.ASSOCIATION, 11L, null);
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(own.getActivite()));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(own, other, section, global));
        when(chats.findById(8L)).thenReturn(Optional.of(own));
        when(chats.findById(9L)).thenReturn(Optional.of(other));
        when(reads.unreadCounts(4L, List.of(8L, 10L))).thenReturn(java.util.Map.of(8L, 3L));
        assertThat(service.rooms("alice")).extracting(ChatService.Room::id).containsExactly(8L, 10L);
        assertThat(service.rooms("alice").getFirst().unreadCount()).isEqualTo(3L);
        assertThat(service.rooms("alice").getFirst().canWrite()).isFalse();
        assertThat(service.history("alice", 8L, null, null)).isEmpty();
        forbidden(() -> service.history("alice", 9L, null, null));
        forbidden(() -> service.send("alice", 8L, "Bonjour"));
    }

    @Test
    void sectionReferentsCanReadEvenAnEmptySection() {
        Chat section = room(ChatTarget.SECTION, 8L, 20L);
        section.getSection().getReferents().add(user);
        when(chats.findById(8L)).thenReturn(Optional.of(section));
        assertThat(service.history("alice", 8L, null, null)).isEmpty();
        section.getSection().getReferents().clear();
        forbidden(() -> service.history("alice", 8L, null, null));
    }

    @ParameterizedTest
    @EnumSource(ERole.class)
    void allExplicitRolePermissionsAreIndependentOfLinksIncludingMemberAndReferentRoles(ERole role) {
        user.setRoles(Set.of(role));
        for (ChatTarget target : ChatTarget.values()) {
            Chat chat = room(target, 8L, target == ChatTarget.ACTIVITE ? 30L : 20L);
            ChatPermission permission = permission(chat, role, false);
            when(chats.findById(8L)).thenReturn(Optional.of(chat));
            when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
            assertThat(service.rooms("alice")).extracting(ChatService.Room::id).containsExactly(8L);
            assertThat(service.history("alice", 8L, null, null)).isEmpty();
            forbidden(() -> service.send("alice", 8L, "Bonjour"));
            permission.setEcriture(true);
            assertThat(service.send("alice", 8L, " Bonjour ").content()).isEqualTo("Bonjour");
            permission.setRole(ERole.ROLE_USER == role ? "ROLE_ADMIN" : "ROLE_USER");
            forbidden(() -> service.history("alice", 8L, null, null));
            forbidden(() -> service.markRead("alice", 8L, 1L));
        }
    }

    @Test
    void revokingLinkRevokesDirectAccessUnlessAnIndependentRoleStillGrantsIt() {
        Chat chat = room(ChatTarget.ACTIVITE, 8L, 30L);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(chat.getActivite()), List.of());
        assertThat(service.history("alice", 8L, null, null)).isEmpty();
        forbidden(() -> service.history("alice", 8L, null, null));
        permission(chat, ERole.ROLE_USER, true);
        assertThat(service.send("alice", 8L, "Toujours autorisé").content()).isEqualTo("Toujours autorisé");
    }

    @ParameterizedTest
    @EnumSource(value = ERole.class, names = {"ROLE_ADMIN", "ROLE_BUREAU", "ROLE_MEMBRECA", "ROLE_SECRETAIRE", "ROLE_ENCADRANT"})
    void rolesAloneNeverGrantAccessToUnrelatedChats(ERole role) {
        user.setRoles(Set.of(role));
        for (ChatTarget target : ChatTarget.values()) {
            when(chats.findById(8L)).thenReturn(Optional.of(room(target, 8L, 20L)));
            forbidden(() -> service.history("alice", 8L, null, null));
            forbidden(() -> service.send("alice", 8L, "Bonjour"));
            forbidden(() -> service.markRead("alice", 8L, 1L));
        }
        verifyNoInteractions(messages, reads);
    }

    @Test
    void readCursorMustBelongToTheAuthorizedChat() {
        Chat chat = room(ChatTarget.ASSOCIATION, 8L, null);
        permission(chat, ERole.ROLE_USER, false);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        ChatMessage foreign = message(room(ChatTarget.ASSOCIATION, 9L, null), 1L);
        when(messages.findById(1L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.markRead("alice", 8L, 1L)).isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(reads);
        foreign.setChat(chat);
        when(reads.unreadCounts(4L, List.of(8L))).thenReturn(java.util.Map.of(8L, 2L));
        assertThat(service.markRead("alice", 8L, 1L).unreadCount()).isEqualTo(2);
        verify(reads).markRead(4L, 8L, 1L);
    }

    @Test
    void preservesHistoryOrderAndRejectsInvalidPaginationAndContent() {
        Chat chat = room(ChatTarget.ASSOCIATION, 8L, null);
        permission(chat, ERole.ROLE_USER, true);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(messages.findByChatIdOrderByIdDesc(eq(8L), any())).thenReturn(List.of(message(chat, 3L), message(chat, 2L)));
        assertThat(service.history("alice", 8L, null, null)).extracting(ChatService.Message::id).containsExactly(2L, 3L);
        assertThatThrownBy(() -> service.history("alice", 8L, 1L, 2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.history("alice", 8L, 0L, null)).isInstanceOf(ResponseStatusException.class);
        for (String content : new String[] {null, " ", "x".repeat(2001)}) {
            assertThatThrownBy(() -> service.send("alice", 8L, content)).isInstanceOf(ResponseStatusException.class);
        }
        verify(messages, never()).save(any());
    }

    private Chat room(ChatTarget target, Long id, Long targetId) {
        Chat chat = new Chat();
        chat.setId(id); chat.setNom("Chat " + id); chat.setCible(target);
        if (target == ChatTarget.SECTION) {
            chat.setSection(Section.builder().id(targetId).nom("Section").build());
        } else if (target == ChatTarget.ACTIVITE) {
            Activite activity = new Activite(); activity.setId(targetId); activity.setNom("Activité");
            activity.setSection(Section.builder().id(20L).nom("Section").build());
            chat.setActivite(activity);
        }
        return chat;
    }

    private ChatPermission permission(Chat chat, ERole role, boolean writing) {
        ChatPermission permission = new ChatPermission();
        permission.setChat(chat); permission.setRole(role.name()); permission.setEcriture(writing);
        chat.getPermissions().add(permission); return permission;
    }

    private ChatMessage message(Chat chat, Long id) {
        ChatMessage message = new ChatMessage(); message.setChat(chat); message.setId(id);
        message.setContent("Bonjour"); message.setCreatedAt(Instant.now()); return message;
    }

    private void forbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }
}
