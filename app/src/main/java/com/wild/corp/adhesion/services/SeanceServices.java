package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.SeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@Transactional
public class SeanceServices {

    @Autowired
    SeanceRepository seanceRepository;

    private final DatasetApi datasetApi;
    private final String zone;
    private final String vacancesDataset;
    private final String joursFeriesDataset;

    public SeanceServices(DatasetApi datasetApi,
                          @Value("${adhesion.calendrier.zone:C}") String zone,
                          @Value("${adhesion.calendrier.vacances-dataset:fr-en-calendrier-scolaire}") String vacancesDataset,
                          @Value("${adhesion.calendrier.jours-feries-dataset:jours-feries-en-france}") String joursFeriesDataset) {
        this.datasetApi = datasetApi;
        this.zone = zone;
        this.vacancesDataset = vacancesDataset;
        this.joursFeriesDataset = joursFeriesDataset;
    }

    public Seance addFirstSeance(Activite activite, LocalDate date) {
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setActivite(activite);
        nouvelleSeance.setDebut(LocalDateTime.of(date, activite.getHoraireDebut()));
        nouvelleSeance.setFin(nouvelleSeance.getDebut().plusMinutes(activite.getDuree()));
        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);
        return nouvelleSeance;
    }

    public void fillSeances(Activite activite, int nbWeeks) {
        fillSeances(activite, nbWeeks, zone, LocalDate.now());
    }

    /** Creates one session per week, except during holidays for the requested school zone. */
    public void fillSeances(Activite activite, int nbWeeks, String schoolZone, LocalDate from) {
        if (activite.getJour() == null || activite.getHoraireDebut() == null || activite.getDuree() == null) {
            throw new IllegalArgumentException("Le jour, l'heure de début et la durée de l'activité sont obligatoires");
        }
        if (nbWeeks < 0) {
            throw new IllegalArgumentException("Le nombre de semaines ne peut pas être négatif");
        }
        if (nbWeeks == 0) {
            return;
        }

        LocalDate firstDate = from.with(TemporalAdjusters.nextOrSame(activite.getJour()));
        LocalDate lastDate = firstDate.plusWeeks(Math.max(0, nbWeeks - 1L));
        Set<LocalDate> unavailableDates = getUnavailableDates(schoolZone, firstDate, lastDate);

        for (int i = 0; i < nbWeeks; i++) {
            LocalDate date = firstDate.plusWeeks(i);
            if (!unavailableDates.contains(date)) {
                activite.getSeances().add(addFirstSeance(activite, date));
            }
        }
    }

    private Set<LocalDate> getUnavailableDates(String schoolZone, LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        String vacationWhere = "zones=\"Zone " + schoolZone.toUpperCase() + "\" and start_date <= date'" + end
                + "' and end_date >= date'" + start + "'";
        Records vacations = response(vacancesDataset, vacationWhere);
        for (Record record : vacations.getResults()) {
            LocalDate vacationStart = date(record, "start_date");
            LocalDate vacationEnd = date(record, "end_date");
            if (vacationStart != null && vacationEnd != null) {
                LocalDate cursor = vacationStart.isBefore(start) ? start : vacationStart;
                LocalDate includedEnd = vacationEnd.minusDays(1).isAfter(end) ? end : vacationEnd.minusDays(1);
                while (!cursor.isAfter(includedEnd)) {
                    dates.add(cursor);
                    cursor = cursor.plusDays(1);
                }
            }
        }

        Records holidays = response(joursFeriesDataset,
                "date >= date'" + start + "' and date <= date'" + end + "'");
        holidays.getResults().stream().map(record -> date(record, "date")).filter(java.util.Objects::nonNull)
                .forEach(dates::add);
        return dates;
    }

    private Records response(String dataset, String where) {
        Records records = datasetApi.getRecords(dataset, null, where, null, null, 100, 0,
                null, null, "fr", "Europe/Paris", false, false).getBody();
        if (records == null) {
            throw new IllegalStateException("La source de calendrier " + dataset + " n'a retourné aucune donnée");
        }
        return records;
    }

    private LocalDate date(Record record, String field) {
        Object value = record.getAdditionalProperty(field);
        if (value == null) {
            return null;
        }
        String isoDate = value.toString();
        return LocalDate.parse(isoDate.substring(0, Math.min(10, isoDate.length())));
    }

    public void modifyDay(Activite activite){
        List<Seance> seances  = activite.getSeances().stream().filter(seance -> seance.getEtatSeance().equals(ESeance.PROGRAMMEE)).toList();
        int nbSeancesRestantes = seances.size();
        seances.forEach(seanceRepository::delete);
        activite.getSeances().removeAll(seances);
        fillSeances(activite, nbSeancesRestantes);
    }
}
