package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeanceServicesTest {

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
    }

    private Activite activity() {
        Activite activite = new Activite();
        activite.setJour(DayOfWeek.MONDAY);
        activite.setHoraireDebut(LocalTime.of(18, 30));
        activite.setDuree(90L);
        return activite;
    }
}
