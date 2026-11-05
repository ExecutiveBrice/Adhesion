package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventCalendarServiceTest {
    @Test void excludesVacationsUntilTheDayBeforeSchoolResumesAndIncludesPublicHolidays() {
        Record vacation = new Record().putAdditionalProperty("start_date", "2026-10-17T00:00:00")
                .putAdditionalProperty("end_date", "2026-11-02T00:00:00");
        Record holiday = new Record().putAdditionalProperty("date", "2026-11-11");
        DatasetApi vacations = mock(DatasetApi.class, invocation -> ResponseEntity.ok(new Records().results(List.of(vacation))));
        DatasetApi holidays = mock(DatasetApi.class, invocation -> ResponseEntity.ok(new Records().results(List.of(holiday))));
        var service = new EventCalendarService(vacations, holidays, "B", "vacations", "holidays");
        var excluded = service.unavailable(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 15));
        assertThat(excluded).contains(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11))
                .doesNotContain(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 11, 2));
    }
    @Test void readsEveryPageAndDoesNotSilentlyIgnoreCalendarFailures() {
        Record holiday = new Record().putAdditionalProperty("date", "2026-11-11");
        DatasetApi vacations = mock(DatasetApi.class, invocation -> ResponseEntity.ok(new Records()));
        List<Integer> offsets = new ArrayList<>();
        DatasetApi holidays = mock(DatasetApi.class, invocation -> {
            int offset = invocation.getArgument(6); offsets.add(offset);
            return ResponseEntity.ok(new Records().results(offset == 0 ? Collections.nCopies(100, holiday) : List.of(holiday)));
        });
        var service = new EventCalendarService(vacations, holidays, "B", "vacations", "holidays");
        service.unavailable(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(offsets).containsExactly(0, 100);
        DatasetApi broken = mock(DatasetApi.class, invocation -> { throw new IllegalStateException("Offline"); });
        var unavailable = new EventCalendarService(broken, holidays, "B", "vacations", "holidays");
        assertThatThrownBy(() -> unavailable.unavailable(LocalDate.now(), LocalDate.now().plusWeeks(4)))
                .hasMessageContaining("503").hasMessageContaining("Aucun événement");
    }
}
