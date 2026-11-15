package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.ConvocationEquipeRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PendingEventRepliesTest {
    @Test void includesEligibleResponsibilitiesAndParticipantsOnlyOncePerEvent() {
        var activity = new Activite(); activity.setId(1L); activity.setNom("Basket");
        var person = new Adherent(); person.setId(2L);
        var membership = new Adhesion(); membership.setActivite(activity); membership.setAdherent(person);
        membership.setStatutActuel(Status.VALIDEE.label);
        var event = new Seance(); event.setId(3L); event.setActivite(activity);
        event.setDebut(LocalDateTime.now().plusDays(30)); event.setFin(event.getDebut().plusHours(1));
        event.setEtatSeance(ESeance.PROGRAMMEE);
        var participant = new ConvocationEquipe(); participant.setSeance(event);
        participant.setAdherent(person); participant.setAdhesion(membership);
        var task = new TacheSeance(); task.setId(4L); task.setNom("Accueil");
        var responsibility = new ResponsabiliteSeance(task, person);
        event.getResponsabilites().add(responsibility);
        var invitation = new ConvocationEquipe(); invitation.setSeance(event); invitation.setAdherent(person);
        invitation.setObjet(ConvocationEquipe.responsibilityKey(responsibility));
        var stale = new ConvocationEquipe(); stale.setSeance(event); stale.setAdherent(person);
        stale.setObjet("RESPONSABILITE:999:0");
        var repository = mock(ConvocationEquipeRepository.class);
        when(repository.findPendingForUser(eq("alice"), any(), anyList()))
                .thenReturn(List.of(participant, invitation, stale));
        var service = new SeanceServices(); service.convocations = repository;

        assertThat(service.getEvenementsAttenteReponse("alice")).extracting(e -> e.id()).containsExactly(3L);
        event.setSansInvitation(true);
        assertThat(service.getEvenementsAttenteReponse("alice")).hasSize(1);
        event.getResponsabilites().clear();
        assertThat(service.getEvenementsAttenteReponse("alice")).isEmpty();
        event.getResponsabilites().add(responsibility);
        event.setSansInvitation(false);
        membership.setStatutActuel(Status.ANNULEE.label);
        assertThat(service.getEvenementsAttenteReponse("alice")).hasSize(1);
        event.getResponsabilites().clear();
        assertThat(service.getEvenementsAttenteReponse("alice")).isEmpty();
    }
}
