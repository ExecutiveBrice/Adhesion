package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PresenceServicesTest {

    private final PresenceServices presenceServices = new PresenceServices();

    @Test
    void createsAPresenceForEachExistingSessionWhenAnAdhesionIsValidated() {
        Activite activite = new Activite();
        Seance firstSeance = session(activite, 1L);
        Seance secondSeance = session(activite, 2L);
        activite.getSeances().add(firstSeance);
        activite.getSeances().add(secondSeance);
        Adhesion adhesion = adhesion(activite, Status.VALIDEE.label);

        presenceServices.fillPresences(adhesion);

        assertThat(adhesion.getPresences()).hasSize(2);
        assertThat(firstSeance.getPresences()).singleElement().satisfies(presence ->
                assertThat(presence.getAdhesion()).isSameAs(adhesion));
        assertThat(secondSeance.getPresences()).singleElement().satisfies(presence ->
                assertThat(presence.getAdhesion()).isSameAs(adhesion));
    }

    @Test
    void createsPresencesOnlyForScheduledSessionsWhenAnAdhesionIsValidated() {
        Activite activite = new Activite();
        Seance scheduledSeance = session(activite, 1L);
        Seance completedSeance = session(activite, 2L);
        completedSeance.setEtatSeance(ESeance.REALISEE);
        Seance cancelledSeance = session(activite, 3L);
        cancelledSeance.setEtatSeance(ESeance.ANNULEE);
        activite.getSeances().addAll(java.util.List.of(scheduledSeance, completedSeance, cancelledSeance));
        Adhesion adhesion = adhesion(activite, Status.VALIDEE.label);

        presenceServices.fillPresences(adhesion);

        assertThat(scheduledSeance.getPresences()).singleElement();
        assertThat(completedSeance.getPresences()).isEmpty();
        assertThat(cancelledSeance.getPresences()).isEmpty();
    }

    @Test
    void createsAPresenceForEachValidatedAdhesionWhenASessionIsAdded() {
        Activite activite = new Activite();
        Adhesion validatedAdhesion = adhesion(activite, Status.VALIDEE.label);
        Adhesion pendingAdhesion = adhesion(activite, Status.ATTENTE_ADHERENT.label);
        activite.getAdhesions().add(validatedAdhesion);
        activite.getAdhesions().add(pendingAdhesion);
        Seance seance = session(activite, 1L);

        presenceServices.fillPresences(seance);

        assertThat(seance.getPresences()).singleElement().satisfies(presence -> {
            assertThat(presence.getAdhesion()).isSameAs(validatedAdhesion);
            assertThat(presence.getSeance()).isSameAs(seance);
        });
        assertThat(validatedAdhesion.getPresences()).hasSize(1);
        assertThat(pendingAdhesion.getPresences()).isEmpty();
    }

    @Test
    void doesNotCreateDuplicatePresences() {
        Activite activite = new Activite();
        Seance seance = session(activite, 3L);
        activite.getSeances().add(seance);
        Adhesion adhesion = adhesion(activite, Status.VALIDEE.label);

        presenceServices.fillPresences(adhesion);
        presenceServices.fillPresences(adhesion);

        assertThat(adhesion.getPresences()).singleElement();
        assertThat(seance.getPresences()).singleElement();
    }

    @Test
    void addsANewParticipantAsPresentForTheCurrentSession() {
        PresenceRepository presenceRepository = mock(PresenceRepository.class);
        presenceServices.presenceRepository = presenceRepository;
        Activite activite = new Activite();
        Adhesion adhesion = adhesion(activite, Status.ATTENTE_ADHERENT.label);
        Seance seance = session(activite, 1L);
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Presence presence = presenceServices.addPresenceForSeance(adhesion, seance, true);

        assertThat(presence.getAdhesion()).isSameAs(adhesion);
        assertThat(presence.getSeance()).isSameAs(seance);
        assertThat(presence.getPresence()).isTrue();
    }

    private Adhesion adhesion(Activite activite, String statut) {
        Adhesion adhesion = new Adhesion();
        adhesion.setActivite(activite);
        adhesion.setStatutActuel(statut);
        return adhesion;
    }

    private Seance session(Activite activite, Long id) {
        Seance seance = new Seance();
        seance.setId(id);
        seance.setActivite(activite);
        seance.setEtatSeance(ESeance.PROGRAMMEE);
        return seance;
    }
}
