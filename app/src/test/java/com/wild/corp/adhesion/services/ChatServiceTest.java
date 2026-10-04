package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ChatTarget;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.SectionChat;
import com.wild.corp.adhesion.models.SectionChatPermission;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.ChatMessageRepository;
import com.wild.corp.adhesion.repository.ChatReadRepository;
import com.wild.corp.adhesion.repository.SectionChatRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void listsOnlyChatsInTheMembersSectionsAlongsideAssociationChats() {
        SectionChat own = sectionChat(8L, 20L);
        SectionChat other = sectionChat(9L, 21L);
        SectionChat association = associationChat(10L, "Association");
        // A different activity with a distinct Section instance but the same persistent ID.
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(activityInSection(20L)));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(own, other, association));
        when(reads.sectionUnreadCounts(4L, List.of(8L, 10L))).thenReturn(java.util.Map.of(8L, 3L));

        assertThat(service.rooms(user.getUsername())).extracting(ChatService.Room::sectionChatId)
                .containsExactly(8L, 10L);
        assertThat(service.sectionRooms(user.getUsername())).extracting(ChatService.SectionRoom::id)
                .containsExactly(8L, 10L);
        verify(reads, never()).sectionUnreadCounts(eq(4L), eq(List.of(8L, 9L, 10L)));
    }

    @ParameterizedTest
    @EnumSource(value = ERole.class, names = {"ROLE_USER"})
    void rejectsSectionAccessWithoutAnActivityEvenWithAPermittedRole(ERole role) {
        user.setRoles(java.util.Set.of(role));
        SectionChat chat = sectionChat(8L, 20L);
        chat.getPermissions().getFirst().setRole(role.name());
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(activityInSection(21L)));

        assertThat(service.rooms(user.getUsername())).isEmpty();
        assertThat(service.sectionRooms(user.getUsername())).isEmpty();
        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
        assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
        assertForbidden(() -> service.markSectionRead(user.getUsername(), 8L, 1L));
        verifyNoInteractions(messages);
        verify(reads, never()).markSectionRead(any(), any(), any());
    }

    @Test
    void allowsSectionHistoryForAMemberButStillHonorsReadOnlyPermissions() {
        SectionChat chat = sectionChat(8L, 20L);
        chat.getPermissions().getFirst().setEcriture(false);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(activityInSection(20L)));
        when(messages.findBySectionChatIdOrderByIdDesc(eq(8L), any())).thenReturn(List.of());

        assertThat(service.sectionHistory(user.getUsername(), 8L, null, null)).isEmpty();
        assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isFalse();
        assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
    }

    @Test
    void sectionMembershipDoesNotReplaceConfiguredRolePermissions() {
        SectionChat chat = sectionChat(8L, 20L);
        chat.getPermissions().clear();
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(activityInSection(20L)));

        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
    }

    @Test
    void activitiesWithoutASectionDoNotGrantSectionAccess() {
        when(chats.findById(8L)).thenReturn(Optional.of(sectionChat(8L, 20L)));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(new Activite()));

        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
    }

    @Test
    void activityChatsRequireTheExactActivityEvenWithinTheSameSection() {
        SectionChat own = activityChat(8L, 30L);
        SectionChat other = activityChat(9L, 31L);
        SectionChat section = sectionChat(10L, 20L);
        SectionChat association = associationChat(11L, "Association");
        Activite memberActivity = activityInSection(20L);
        memberActivity.setId(30L);
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(memberActivity));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(own, other, section, association));
        when(reads.sectionUnreadCounts(4L, List.of(8L, 10L, 11L))).thenReturn(java.util.Map.of(8L, 2L));

        assertThat(service.rooms(user.getUsername())).extracting(ChatService.Room::sectionChatId)
                .containsExactly(8L, 10L, 11L);
        assertThat(service.sectionRooms(user.getUsername())).extracting(ChatService.SectionRoom::id)
                .containsExactly(8L, 10L, 11L);
    }

    @ParameterizedTest
    @EnumSource(value = ERole.class, names = {"ROLE_USER"})
    void rejectsActivityAccessWithoutMembershipEvenWithAPermittedRole(ERole role) {
        user.setRoles(java.util.Set.of(role));
        SectionChat chat = activityChat(8L, 30L);
        chat.getPermissions().getFirst().setRole(role.name());
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(activityChat(9L, 31L).getActivite()));

        assertThat(service.rooms(user.getUsername())).isEmpty();
        assertThat(service.sectionRooms(user.getUsername())).isEmpty();
        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
        assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
        assertForbidden(() -> service.markSectionRead(user.getUsername(), 8L, 1L));
        verifyNoInteractions(messages);
        verify(reads, never()).markSectionRead(any(), any(), any());
    }

    @Test
    void activityMembershipStillHonorsReadAndWritePermissions() {
        SectionChat chat = activityChat(8L, 30L);
        chat.getPermissions().getFirst().setEcriture(false);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(chat.getActivite()));
        when(messages.findBySectionChatIdOrderByIdDesc(eq(8L), any())).thenReturn(List.of());

        assertThat(service.sectionHistory(user.getUsername(), 8L, null, null)).isEmpty();
        assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isFalse();
        assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
        chat.getPermissions().clear();
        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
    }

    @Test
    void revokingActivityMembershipAlsoRevokesDirectAccess() {
        SectionChat chat = activityChat(8L, 30L);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(activities.findChatActivities(eq(4L), anyList()))
                .thenReturn(List.of(chat.getActivite()), List.of());
        when(messages.findBySectionChatIdOrderByIdDesc(eq(8L), any())).thenReturn(List.of());

        assertThat(service.sectionHistory(user.getUsername(), 8L, null, null)).isEmpty();
        assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
        verify(activities, org.mockito.Mockito.times(2)).findChatActivities(4L,
                List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label));
    }

    @ParameterizedTest
    @EnumSource(value = ERole.class, names = {"ROLE_USER"}, mode = EnumSource.Mode.EXCLUDE)
    void otherRolesUseConfiguredVisibilityIndependentlyOfMembership(ERole role) {
        user.setRoles(java.util.Set.of(ERole.ROLE_USER, role));
        for (SectionChat chat : List.of(sectionChat(8L, 20L), activityChat(8L, 30L), associationChat(8L, "Global"))) {
            chat.getPermissions().getFirst().setRole(role.name());
            chat.getPermissions().getFirst().setEcriture(false);
            when(chats.findById(8L)).thenReturn(Optional.of(chat));
            when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));
            when(messages.findBySectionChatIdOrderByIdDesc(eq(8L), any())).thenReturn(List.of());

            assertThat(service.rooms(user.getUsername())).extracting(ChatService.Room::sectionChatId).containsExactly(8L);
            assertThat(service.sectionRooms(user.getUsername())).extracting(ChatService.SectionRoom::id).containsExactly(8L);
            assertThat(service.sectionHistory(user.getUsername(), 8L, null, null)).isEmpty();
            assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isFalse();
            assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
            chat.getPermissions().getFirst().setEcriture(true);
            assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isTrue();
            when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(service.sendSectionMessage(user.getUsername(), 8L, "Bonjour").content()).isEqualTo("Bonjour");

            chat.getPermissions().clear();
            assertThat(service.rooms(user.getUsername())).isEmpty();
            assertForbidden(() -> service.sectionHistory(user.getUsername(), 8L, null, null));
            assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));
            assertForbidden(() -> service.markSectionRead(user.getUsername(), 8L, 1L));
        }
    }

    @Test
    void scopedMemberWritePermissionDoesNotOverrideAnotherRolesReadOnlyAccess() {
        user.setRoles(java.util.Set.of(ERole.ROLE_USER, ERole.ROLE_ENCADRANT));
        SectionChat chat = activityChat(8L, 30L);
        SectionChatPermission permission = new SectionChatPermission();
        permission.setChat(chat);
        permission.setRole(ERole.ROLE_ENCADRANT.name());
        permission.setEcriture(false);
        chat.getPermissions().add(permission);
        when(chats.findById(8L)).thenReturn(Optional.of(chat));
        when(chats.findAllByOrderByNomAsc()).thenReturn(List.of(chat));

        assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isFalse();
        assertForbidden(() -> service.sendSectionMessage(user.getUsername(), 8L, "Bonjour"));

        when(activities.findChatActivities(eq(4L), anyList())).thenReturn(List.of(chat.getActivite()));
        assertThat(service.rooms(user.getUsername()).getFirst().canWrite()).isTrue();
    }

    private SectionChat activityChat(Long id, Long activityId) {
        SectionChat chat = associationChat(id, "Chat " + id);
        chat.setCible(ChatTarget.ACTIVITE);
        Activite activity = activityInSection(20L);
        activity.setId(activityId);
        activity.setNom("Activité " + activityId);
        chat.setActivite(activity);
        return chat;
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    private Activite activityInSection(Long sectionId) {
        Section section = new Section();
        section.setId(sectionId);
        section.setNom("Section " + sectionId);
        Activite activity = new Activite();
        activity.setSection(section);
        return activity;
    }

    private SectionChat sectionChat(Long id, Long sectionId) {
        SectionChat chat = associationChat(id, "Chat " + id);
        chat.setCible(ChatTarget.SECTION);
        chat.setSection(activityInSection(sectionId).getSection());
        return chat;
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
