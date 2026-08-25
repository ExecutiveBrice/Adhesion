package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicesTest {

    @Test
    void returnsOnlyTodaySessionsForTheActivitiesManagedByTheUser() {
        SeanceRepository seanceRepository = mock(SeanceRepository.class);
        UserServices userServices = new UserServices();
        userServices.seanceRepository = seanceRepository;

        Activite activite = new Activite();
        activite.setNom("Pilates");
        Seance afternoon = seance(activite, LocalTime.of(18, 30));
        Seance morning = seance(activite, LocalTime.of(9, 0));

        Adhesion validatedAdhesion = new Adhesion();
        validatedAdhesion.setStatutActuel(Status.VALIDEE.label);
        activite.getAdhesions().add(validatedAdhesion);
        when(seanceRepository.findTodayByProfessorUsername(
                eq("prof@alod.fr"), eq(LocalDate.now().atStartOfDay()), eq(LocalDate.now().plusDays(1).atStartOfDay())))
                .thenReturn(java.util.List.of(morning, afternoon));

        var sessions = userServices.getSeancesDuJourForUser("prof@alod.fr");

        assertThat(sessions).extracting(session -> session.debut().toLocalTime())
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(18, 30));
        assertThat(sessions).allSatisfy(session -> {
            assertThat(session.activite()).isEqualTo("Pilates");
            assertThat(session.nombreParticipants()).isEqualTo(1);
        });
        verify(seanceRepository).findTodayByProfessorUsername(
                "prof@alod.fr", LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
    }

    @Test
    void returnsEverySessionForTheDaySelectedByTheSecretary() {
        SeanceRepository seanceRepository = mock(SeanceRepository.class);
        UserServices userServices = new UserServices();
        userServices.seanceRepository = seanceRepository;

        LocalDate selectedDay = LocalDate.of(2026, 9, 2);
        Activite activite = new Activite();
        activite.setNom("Yoga");
        Seance morning = seance(activite, LocalTime.of(9, 0));
        morning.setDebut(LocalDateTime.of(selectedDay, LocalTime.of(9, 0)));
        morning.setFin(morning.getDebut().plusHours(1));

        when(seanceRepository.findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
                selectedDay.atStartOfDay(), selectedDay.plusDays(1).atStartOfDay()))
                .thenReturn(java.util.List.of(morning));

        var sessions = userServices.getSeancesDuJourForSecretary(selectedDay);

        assertThat(sessions).singleElement().satisfies(session -> {
            assertThat(session.activite()).isEqualTo("Yoga");
            assertThat(session.debut()).isEqualTo(morning.getDebut());
        });
        verify(seanceRepository).findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
                selectedDay.atStartOfDay(), selectedDay.plusDays(1).atStartOfDay());
    }

    private Seance seance(Activite activite, LocalTime heure) {
        Seance seance = new Seance();
        seance.setActivite(activite);
        seance.setEtatSeance(ESeance.PROGRAMMEE);
        seance.setDebut(LocalDateTime.of(LocalDate.now(), heure));
        seance.setFin(seance.getDebut().plusHours(1));
        return seance;
    }
}
