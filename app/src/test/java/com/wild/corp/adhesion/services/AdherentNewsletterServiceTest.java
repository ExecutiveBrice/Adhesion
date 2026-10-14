package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdherentNewsletterServiceTest {
    private final AdherentRepository adherents = mock(AdherentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NewsletterContactService contacts = mock(NewsletterContactService.class);
    private final AdherentNewsletterService service = new AdherentNewsletterService(adherents, users, contacts);
    private final User owner = new User("owner@example.org", "password");
    private final Adherent member = new Adherent();

    private UsernamePasswordAuthenticationToken authentication(String role) {
        return new UsernamePasswordAuthenticationToken(owner.getUsername(), "password", AuthorityUtils.createAuthorityList(role));
    }

    @BeforeEach
    void setup() {
        owner.setId(1L); owner.setEmailValid(true);
        member.setId(7L); member.setUser(owner); owner.setAdherent(member);
        when(users.findByUsername(owner.getUsername())).thenReturn(Optional.of(owner));
        when(adherents.findById(7L)).thenReturn(Optional.of(member));
    }

    @Test
    void ownerReadsAndUpdatesOnlySavedEmail() {
        when(contacts.getStatus(owner.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        assertThat(service.status(7L, authentication("ROLE_USER")).canModify()).isTrue();
        when(contacts.unsubscribeFromNewsletter(owner.getUsername())).thenReturn(NewsletterContactService.Status.NOT_SUBSCRIBED);
        assertThat(service.update(7L, authentication("ROLE_USER"), false, owner.getUsername()).status())
                .isEqualTo(NewsletterContactService.Status.NOT_SUBSCRIBED);
        verify(contacts).unsubscribeFromNewsletter(owner.getUsername());
    }

    @Test
    void unrelatedMemberCannotReadOrMutateAndOtherStaffOnlyReads() {
        User other = new User("other@example.org", "password"); other.setId(2L);
        member.setUser(other);
        assertThatThrownBy(() -> service.status(7L, authentication("ROLE_USER"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(7L, authentication("ROLE_USER"), true, other.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
        when(contacts.getStatus(other.getUsername())).thenReturn(NewsletterContactService.Status.NOT_FOUND);
        assertThat(service.status(7L, authentication("ROLE_BUREAU")).canModify()).isFalse();
        assertThatThrownBy(() -> service.update(7L, authentication("ROLE_BUREAU"), true, other.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
        verify(contacts, never()).subscribeByOwner(anyString());
    }

    @Test
    void administratorsAndSecretariesCanSubscribeAndUnsubscribeFromAnotherMembersModal() {
        User other = new User("other@example.org", "password"); other.setId(2L); other.setEmailValid(false);
        member.setUser(other);
        when(contacts.getStatus(other.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        when(contacts.subscribeByOwner(other.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        when(contacts.unsubscribeFromNewsletter(other.getUsername())).thenReturn(NewsletterContactService.Status.NOT_SUBSCRIBED);
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_SECRETAIRE"}) {
            var subscription = service.status(7L, authentication(role));
            assertThat(subscription.canModify()).isTrue();
            assertThat(subscription.canSubscribe()).isTrue();
            assertThat(service.update(7L, authentication(role), false, other.getUsername()).status())
                    .isEqualTo(NewsletterContactService.Status.NOT_SUBSCRIBED);
            assertThat(service.update(7L, authentication(role), true, other.getUsername()).status())
                    .isEqualTo(NewsletterContactService.Status.SUBSCRIBED);
        }
        verify(contacts, times(2)).unsubscribeFromNewsletter(other.getUsername());
        verify(contacts, times(2)).subscribeByOwner(other.getUsername());
    }

    @Test
    void householdCanReadAnotherAdultButCannotChangeTheirSubscription() {
        Adherent viewerMember = new Adherent();
        Tribu family = new Tribu(); family.setId(3L);
        viewerMember.setTribu(family); owner.setAdherent(viewerMember); member.setTribu(family);
        User adult = new User("adult@example.org", "password"); adult.setId(2L); member.setUser(adult);
        when(contacts.getStatus(adult.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        assertThat(service.status(7L, authentication("ROLE_USER")).canModify()).isFalse();
        assertThatThrownBy(() -> service.update(7L, authentication("ROLE_USER"), false, adult.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
        verify(contacts, never()).unsubscribeFromNewsletter(anyString());
    }

    @Test
    void representativeEmailCannotBeManagedFromChildsModalEvenByStaff() {
        Adherent parent = new Adherent(); parent.setUser(owner);
        member.setRepresentant(parent); member.setEmailRepresentant(true);
        for (String role : new String[]{"ROLE_USER", "ROLE_ADMIN", "ROLE_SECRETAIRE"}) {
            for (boolean subscribed : new boolean[]{true, false}) {
                assertThatThrownBy(() -> service.update(7L, authentication(role), subscribed, owner.getUsername()))
                        .isInstanceOf(AccessDeniedException.class).hasMessageContaining("fiche du représentant");
            }
        }
        verifyNoInteractions(contacts);
        when(contacts.getStatus(owner.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        var result = service.status(7L, authentication("ROLE_ADMIN"));
        assertThat(result.canModify()).isFalse();
        assertThat(result.canSubscribe()).isFalse();
    }

    @Test
    void representedMemberWithOwnEmailCanManageOnlyTheirOwnSubscription() {
        User parentUser = new User("parent@example.org", "password"); parentUser.setId(2L);
        Adherent parent = new Adherent(); parent.setUser(parentUser);
        member.setRepresentant(parent);
        when(contacts.getStatus(owner.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        when(contacts.subscribeByOwner(owner.getUsername())).thenReturn(NewsletterContactService.Status.SUBSCRIBED);
        when(contacts.unsubscribeFromNewsletter(owner.getUsername())).thenReturn(NewsletterContactService.Status.NOT_SUBSCRIBED);
        for (Boolean representativeEmail : new Boolean[]{false, null}) {
            member.setEmailRepresentant(representativeEmail);
            for (String role : new String[]{"ROLE_USER", "ROLE_ADMIN", "ROLE_SECRETAIRE"}) {
                var subscription = service.status(7L, authentication(role));
                assertThat(subscription.email()).isEqualTo(owner.getUsername());
                assertThat(subscription.canModify()).isTrue();
                assertThat(subscription.canSubscribe()).isTrue();
                assertThat(subscription.representativeEmail()).isFalse();
                assertThat(service.update(7L, authentication(role), true, owner.getUsername()).status())
                        .isEqualTo(NewsletterContactService.Status.SUBSCRIBED);
                assertThat(service.update(7L, authentication(role), false, owner.getUsername()).status())
                        .isEqualTo(NewsletterContactService.Status.NOT_SUBSCRIBED);
            }
        }
        verify(contacts, never()).subscribeByOwner(parentUser.getUsername());
        verify(contacts, never()).unsubscribeFromNewsletter(parentUser.getUsername());
    }

    @Test
    void staleEmailAndUnverifiedSubscriptionAreRejectedBeforeBrevoMutation() {
        assertThatThrownBy(() -> service.update(7L, authentication("ROLE_USER"), true, "old@example.org"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        owner.setEmailValid(false);
        assertThatThrownBy(() -> service.update(7L, authentication("ROLE_USER"), true, owner.getUsername()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(contacts);
        when(contacts.unsubscribeFromNewsletter(owner.getUsername())).thenReturn(NewsletterContactService.Status.NOT_SUBSCRIBED);
        assertThat(service.update(7L, authentication("ROLE_USER"), false, owner.getUsername()).canSubscribe()).isFalse();
    }
}
