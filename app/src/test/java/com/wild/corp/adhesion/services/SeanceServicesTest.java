package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Salle;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.repository.SeanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeanceServicesTest {

    @Test
    void exposesPublicCalendarDataWithoutSessionComments() {
        SeanceRepository repository = mock(SeanceRepository.class);
        Activite activite = new Activite();
        activite.setId(5L);
        activite.setNom("Pilates");
        activite.setHoraire("Mardi de 19h30 à 20h30");
        activite.setSalle(Salle.builder().id(4L).nom("Salle des sports").adresse("1 rue des Sports")
                .couleur("#4285F4").build());

        Seance seance = new Seance();
        seance.setId(42L);
        seance.setActivite(activite);
        seance.setSalle(activite.getSalle());
        seance.setDebut(LocalDateTime.of(2026, 9, 1, 19, 30));
        seance.setFin(LocalDateTime.of(2026, 9, 1, 20, 30));
        seance.setEtatSeance(ESeance.PROGRAMMEE);
        seance.setCommentaire("Information interne");

        when(repository.findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
                LocalDateTime.of(2026, 8, 31, 0, 0),
                LocalDateTime.of(2026, 10, 12, 0, 0)))
                .thenReturn(List.of(seance));
        SeanceServices service = serviceWithRepository(repository);

        var calendrier = service.getCalendrier(
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 10, 11));

        assertThat(calendrier).singleElement().satisfies(evenement -> {
            assertThat(evenement.activiteNom()).isEqualTo("Pilates");
            assertThat(evenement.salle()).isEqualTo("Salle des sports");
            assertThat(evenement.adresseSalle()).isEqualTo("1 rue des Sports");
            assertThat(evenement.couleurSalle()).isEqualTo("#4285F4");
            assertThat(evenement.debut()).isEqualTo(LocalDateTime.of(2026, 9, 1, 19, 30));
        });
    }

    @Test
    void createsWeeklySessionsAtActivityStartAndSkipsVacationsAndPublicHolidays() {
        DatasetApi vacancesApi = mock(DatasetApi.class);
        DatasetApi joursFeriesApi = mock(DatasetApi.class);
        Records vacations = new Records().results(List.of(new Record()
                .putAdditionalProperty("start_date", "2026-09-12T00:00:00+02:00")
                .putAdditionalProperty("end_date", "2026-09-15T00:00:00+02:00")));
        Records holidays = new Records().results(List.of(new Record()
                .putAdditionalProperty("date", "2026-09-21")));

        when(vacancesApi.getRecords(eq("vacances"), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq(100), eq(0), nullable(String.class),
                nullable(String.class), eq("fr"), eq("Europe/Paris"), eq(false), eq(false)))
                .thenReturn(ResponseEntity.ok(vacations));
        when(joursFeriesApi.getRecords(eq("feries"), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq(100), eq(0), nullable(String.class),
                nullable(String.class), eq("fr"), eq("Europe/Paris"), eq(false), eq(false)))
                .thenReturn(ResponseEntity.ok(holidays));

        SeanceServices service = new SeanceServices(vacancesApi, joursFeriesApi, "C", "vacances", "feries");
        Activite activite = activity();
        service.fillSeances(activite, 4, "C", LocalDate.of(2026, 9, 8));

        assertThat(activite.getSeances()).extracting("debut").containsExactly(
                LocalDateTime.of(2026, 9, 28, 18, 30),
                LocalDateTime.of(2026, 10, 5, 18, 30));
        assertThat(activite.getSeances().getFirst().getFin())
                .isEqualTo(LocalDateTime.of(2026, 9, 28, 20, 0));
        verify(joursFeriesApi).getRecords(eq("feries"), nullable(String.class),
                argThat(where -> where.contains("statut=\"férié\"")), nullable(String.class), nullable(String.class),
                eq(100), eq(0), nullable(String.class), nullable(String.class), eq("fr"),
                eq("Europe/Paris"), eq(false), eq(false));
    }

    @Test
    void initializesEachSessionWithTheActivityRoom() {
        SeanceServices service = serviceWithRepository(mock(SeanceRepository.class));
        Activite activite = activity();
        Salle salle = Salle.builder().id(8L).nom("Gymnase").adresse("1 rue du Gymnase").couleur("#123456").build();
        activite.setSalle(salle);

        Seance seance = service.addFirstSeance(activite, LocalDate.of(2026, 9, 7));

        assertThat(seance.getSalle()).isSameAs(salle);
    }

    @Test
    void addsTheRequestedNumberOfSessionsAndSkipsExistingDates() {
        DatasetApi vacancesApi = mock(DatasetApi.class);
        DatasetApi joursFeriesApi = mock(DatasetApi.class);
        Records vacations = new Records().results(List.of(new Record()
                .putAdditionalProperty("start_date", "2026-09-12T00:00:00+02:00")
                .putAdditionalProperty("end_date", "2026-09-15T00:00:00+02:00")));
        Records holidays = new Records().results(List.of(new Record()
                .putAdditionalProperty("date", "2026-09-21")));

        when(vacancesApi.getRecords(eq("vacances"), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq(100), eq(0), nullable(String.class),
                nullable(String.class), eq("fr"), eq("Europe/Paris"), eq(false), eq(false)))
                .thenReturn(ResponseEntity.ok(vacations));
        when(joursFeriesApi.getRecords(eq("feries"), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq(100), eq(0), nullable(String.class),
                nullable(String.class), eq("fr"), eq("Europe/Paris"), eq(false), eq(false)))
                .thenReturn(ResponseEntity.ok(holidays));

        SeanceServices service = new SeanceServices(vacancesApi, joursFeriesApi, "C", "vacances", "feries");
        Activite activite = activity();
        activite.getSeances().add(service.addFirstSeance(activite, LocalDate.of(2026, 9, 28)));

        List<Seance> created = service.addSeances(activite, 2, "C", LocalDate.of(2026, 9, 8));

        assertThat(created).extracting("debut").containsExactly(
                LocalDateTime.of(2026, 10, 5, 18, 30),
                LocalDateTime.of(2026, 10, 12, 18, 30));
        assertThat(activite.getSeances()).hasSize(3);
    }

    @Test
    void updatesTheStateAndCommentForTheRequestedActivitySession() {
        SeanceRepository repository = mock(SeanceRepository.class);
        Seance seance = new Seance();
        seance.setId(7L);
        seance.setEtatSeance(ESeance.PROGRAMMEE);
        seance.setCommentaire("Commentaire conservé");
        when(repository.updateEtat(7L, 3L, ESeance.REALISEE)).thenAnswer(invocation -> {
            seance.setEtatSeance(ESeance.REALISEE);
            return 1;
        });
        when(repository.updateCommentaire(7L, 3L, "Présents au complet")).thenAnswer(invocation -> {
            seance.setCommentaire("Présents au complet");
            return 1;
        });
        when(repository.findByIdAndActivite_Id(7L, 3L)).thenReturn(Optional.of(seance));
        SeanceServices service = serviceWithRepository(repository);

        Seance updated = service.updateSeance(
                3L, 7L, ESeance.REALISEE, null, false, null, null, false, null, false);

        assertThat(updated.getEtatSeance()).isEqualTo(ESeance.REALISEE);
        assertThat(updated.getCommentaire()).isEqualTo("Commentaire conservé");
        service.updateSeance(
                3L, 7L, null, "  Présents au complet  ", true, null, null, false, null, false);
        assertThat(updated.getCommentaire()).isEqualTo("Présents au complet");
        verify(repository).updateEtat(7L, 3L, ESeance.REALISEE);
        verify(repository).updateCommentaire(7L, 3L, "Présents au complet");
    }

    @Test
    void updatesTheDateAndStartTimeWhileKeepingTheSessionDuration() {
        SeanceRepository repository = mock(SeanceRepository.class);
        Seance seance = new Seance();
        seance.setId(11L);
        seance.setDebut(LocalDateTime.of(2026, 9, 1, 8, 15));
        seance.setFin(LocalDateTime.of(2026, 9, 1, 9, 0));
        LocalDateTime nouveauDebut = LocalDateTime.of(2026, 9, 8, 10, 30);
        LocalDateTime nouvelleFin = LocalDateTime.of(2026, 9, 8, 11, 15);
        when(repository.findByIdAndActivite_Id(11L, 3L)).thenReturn(Optional.of(seance));
        when(repository.updateHoraire(11L, 3L, nouveauDebut, nouvelleFin)).thenAnswer(invocation -> {
            seance.setDebut(nouveauDebut);
            seance.setFin(nouvelleFin);
            return 1;
        });
        SeanceServices service = serviceWithRepository(repository);

        Seance updated = service.updateSeance(
                3L, 11L, null, null, false,
                LocalDate.of(2026, 9, 8), LocalTime.of(10, 30), true, null, false);

        assertThat(updated.getDebut()).isEqualTo(nouveauDebut);
        assertThat(updated.getFin()).isEqualTo(nouvelleFin);
        verify(repository).updateHoraire(11L, 3L, nouveauDebut, nouvelleFin);
    }

    @Test
    void deletesOnlyTheRequestedActivitySession() {
        SeanceRepository repository = mock(SeanceRepository.class);
        Seance seance = new Seance();
        seance.setId(9L);
        when(repository.findByIdAndActivite_Id(9L, 4L)).thenReturn(Optional.of(seance));
        SeanceServices service = serviceWithRepository(repository);

        service.deleteSeance(4L, 9L);

        verify(repository).delete(seance);
    }

    private SeanceServices serviceWithRepository(SeanceRepository repository) {
        SeanceServices service = new SeanceServices(
                mock(DatasetApi.class), mock(DatasetApi.class), "C", "vacances", "feries");
        service.seanceRepository = repository;
        return service;
    }

    private Activite activity() {
        Activite activite = new Activite();
        activite.setJour(DayOfWeek.MONDAY);
        activite.setHoraireDebut(LocalTime.of(18, 30));
        activite.setDuree(90L);
        return activite;
    }
}
