package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class TribuSessionServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AdherentRepository adherents = mock(AdherentRepository.class);
    private final JwtUtils jwt = mock(JwtUtils.class);
    private final PwaSessionService pwa = mock(PwaSessionService.class);
    private final TribuSessionService service = new TribuSessionService(users, adherents, jwt, pwa);
    private User actor;
    private Adherent target;

    @BeforeEach
    void prepare() {
        Tribu tribe = new Tribu();
        tribe.setId(1L);
        actor = new User("parent@example.org", "secret");
        actor.setId(10L);
        actor.setRoles(Set.of(ERole.ROLE_ADMIN, ERole.ROLE_USER));
        Adherent source = new Adherent();
        source.setTribu(tribe);
        actor.setAdherent(source);
        User child = new User("child@example.org", "secret");
        child.setId(20L);
        child.setRoles(Set.of(ERole.ROLE_USER));
        target = new Adherent();
        target.setId(2L);
        target.setTribu(tribe);
        target.setUser(child);
        when(users.findById(10L)).thenReturn(Optional.of(actor));
        when(adherents.findById(2L)).thenReturn(Optional.of(target));
        when(jwt.generateJwtToken(any())).thenAnswer(invocation -> {
            UserDetails principal = (UserDetails) ((Authentication) invocation.getArgument(0)).getPrincipal();
            assertThat(principal.getId()).isEqualTo(20L);
            return "child-jwt";
        });
    }

    @Test
    void usesOnlyTargetRolesAndIdentity() {
        var response = service.switchMember(UserDetails.build(actor), 2L, false);
        assertThat(response.getToken()).isEqualTo("child-jwt");
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getRoles()).containsExactly("ROLE_USER");
        verifyNoInteractions(pwa);
    }

    @Test
    void switchesToAMemberWithoutARealEmail() {
        target.getUser().setUsername("adherent-123@sans-email.invalid");

        var response = service.switchMember(UserDetails.build(actor), 2L, false);

        assertThat(response.getToken()).isEqualTo("child-jwt");
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getUsername()).isEqualTo("adherent-123@sans-email.invalid");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void remembersTargetInsteadOfOriginalAccount() {
        when(pwa.issue(any())).thenReturn("child-refresh");
        var response = service.switchMember(UserDetails.build(actor), 2L, true);
        assertThat(response.getRefreshToken()).isEqualTo("child-refresh");
        verify(pwa).issue(argThat(principal -> principal.getId().equals(20L)));
    }

    @Test
    void rejectsAnotherTribeEvenForAdministrator() {
        Tribu other = new Tribu();
        other.setId(99L);
        target.setTribu(other);
        assertThatThrownBy(() -> service.switchMember(UserDetails.build(actor), 2L, false))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(jwt, pwa);
    }

    @Test
    void rejectsMissingTribeAndMissingAccount() {
        actor.getAdherent().setTribu(null);
        assertThatThrownBy(() -> service.switchMember(UserDetails.build(actor), 2L, false))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        actor.getAdherent().setTribu(target.getTribu());
        target.setUser(null);
        assertThatThrownBy(() -> service.switchMember(UserDetails.build(actor), 2L, false))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verifyNoInteractions(jwt, pwa);
    }

    @Test
    void rejectsRevokedSession() {
        UserDetails principal = UserDetails.build(actor);
        actor.setSessionVersion(1);
        assertThatThrownBy(() -> service.switchMember(principal, 2L, false))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(jwt, pwa);
    }
}
