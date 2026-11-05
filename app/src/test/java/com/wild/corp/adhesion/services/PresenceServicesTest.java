package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class PresenceServicesTest {
    private final PresenceServices service = new PresenceServices();

    @Test void registrationAssignsDefaultTeamWithoutRewritingExistingEventParticipants() {
        service.activityTeams = mock(ActivityTeamService.class);
        Adhesion membership = membership(1L, Status.VALIDEE.label);
        Seance past = event(membership.getActivite());
        membership.getActivite().getSeances().add(past);
        service.fillPresences(membership);
        verify(service.activityTeams).assignNewMembership(membership);
        assertThat(past.getPresences()).isEmpty();
    }

    @Test void eventUsesOnlyAssignedEligibleMembersAndDeduplicatesSharedTeams() {
        Adhesion valid = membership(1L, Status.VALIDEE.label);
        Adhesion waiting = membership(2L, Status.LISTE_ATTENTE.label);
        Adhesion cancelled = membership(3L, Status.ANNULEE.label);
        Adhesion unassigned = membership(4L, Status.VALIDEE.label);
        Seance event = event(valid.getActivite());
        for (int i = 0; i < 2; i++) {
            Equipe team = new Equipe(); team.setId((long)i); team.setActivite(valid.getActivite());
            for (Adhesion membership : List.of(valid, waiting, cancelled)) {
                membership.setActivite(valid.getActivite());
                AffectationEquipe a = new AffectationEquipe(); a.setEquipe(team); a.setAdhesion(membership); a.setDebut(LocalDate.now());
                team.getAffectations().add(a);
            }
            event.getEquipes().add(team);
        }
        service.fillPresences(event); service.fillPresences(event);
        assertThat(event.getPresences()).singleElement().satisfies(p -> {
            assertThat(p.getAdhesion()).isSameAs(valid);
            assertThat(p.getPresence()).isNull(); assertThat(p.getPresencePrevue()).isNull();
        });
        assertThat(waiting.getPresences()).isEmpty(); assertThat(cancelled.getPresences()).isEmpty();
        assertThat(unassigned.getPresences()).isEmpty();
    }

    @Test void directAttendanceKeepsTheActualPresenceSeparateFromTheResponse() {
        service.presenceRepository = mock(PresenceRepository.class);
        when(service.presenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Adhesion membership = membership(1L, Status.ATTENTE_ADHERENT.label);
        Presence p = service.addPresenceForSeance(membership, event(membership.getActivite()), true);
        assertThat(p.getPresence()).isTrue(); assertThat(p.getPresencePrevue()).isNull();
    }
    private Adhesion membership(Long id, String status) {
        Adhesion a = new Adhesion(); a.setId(id); a.setActivite(new Activite()); a.setStatutActuel(status);
        Adherent person = new Adherent(); person.setId(id); a.setAdherent(person); return a;
    }
    private Seance event(Activite activity) {
        Seance e = new Seance(); e.setId(9L); e.setActivite(activity); e.setDebut(LocalDateTime.now().plusDays(1)); return e;
    }
}
