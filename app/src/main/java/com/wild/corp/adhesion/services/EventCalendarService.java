package com.wild.corp.adhesion.services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;
import java.time.LocalDate;
import java.util.*;

@Service
public class EventCalendarService {
    private final DatasetApi vacations, holidays;
    private final String zone, vacationDataset, holidayDataset;
    public EventCalendarService(@Qualifier("vacancesDatasetApi") DatasetApi vacations,
            @Qualifier("joursFeriesDatasetApi") DatasetApi holidays,
            @Value("${adhesion.calendrier.zone:B}") String zone,
            @Value("${adhesion.calendrier.vacances-dataset:fr-en-calendrier-scolaire}") String vacationDataset,
            @Value("${adhesion.calendrier.jours-feries-dataset:jours-ouvres-week-end-feries-france-2010-a-2030}") String holidayDataset) {
        this.vacations = vacations; this.holidays = holidays; this.zone = zone;
        this.vacationDataset = vacationDataset; this.holidayDataset = holidayDataset;
    }
    public Set<LocalDate> unavailable(LocalDate start, LocalDate end) {
        try {
            Set<LocalDate> dates = new HashSet<>();
            for (Record record : records(vacations, vacationDataset, "zones=\"Zone " + zone.toUpperCase(Locale.ROOT)
                    + "\" and start_date <= date'" + end + "' and end_date >= date'" + start + "'")) {
                LocalDate from = date(record, "start_date"), until = date(record, "end_date");
                for (LocalDate d = from.isBefore(start) ? start : from; d.isBefore(until) && !d.isAfter(end); d = d.plusDays(1)) dates.add(d);
            }
            for (Record record : records(holidays, holidayDataset,
                    "statut=\"férié\" and date >= date'" + start + "' and date <= date'" + end + "'")) dates.add(date(record, "date"));
            return dates;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Impossible de vérifier les vacances et jours fériés. Aucun événement n’a été créé. Réessayez.", e);
        }
    }
    private List<Record> records(DatasetApi api, String dataset, String where) {
        List<Record> result = new ArrayList<>();
        for (int offset = 0; ; offset += 100) {
            Records page = api.getRecords(dataset, null, where, null, null, 100, offset,
                    null, null, "fr", "Europe/Paris", false, false).getBody();
            if (page == null || page.getResults() == null) throw new IllegalStateException("Calendrier indisponible");
            result.addAll(page.getResults());
            if (page.getResults().size() < 100) return result;
        }
    }
    private LocalDate date(Record record, String field) {
        return LocalDate.parse(Objects.requireNonNull(record.getAdditionalProperty(field)).toString().substring(0, 10));
    }
}
