package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceCalendrierResponse;
import com.wild.corp.adhesion.repository.SeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Transactional
public class SeanceServices {

    @Autowired
    SeanceRepository seanceRepository;

    private final DatasetApi vacancesApi;
    private final DatasetApi joursFeriesApi;
    private final String zone;
    private final String vacancesDataset;
    private final String joursFeriesDataset;

    public SeanceServices(@Qualifier("vacancesDatasetApi") DatasetApi vacancesApi,
                          @Qualifier("joursFeriesDatasetApi") DatasetApi joursFeriesApi,
                          @Value("${adhesion.calendrier.zone:C}") String zone,
                          @Value("${adhesion.calendrier.vacances-dataset:fr-en-calendrier-scolaire}") String vacancesDataset,
                          @Value("${adhesion.calendrier.jours-feries-dataset:jours-ouvres-week-end-feries-france-2010-a-2030}") String joursFeriesDataset) {
        this.vacancesApi = vacancesApi;
        this.joursFeriesApi = joursFeriesApi;
        this.zone = zone;
        this.vacancesDataset = vacancesDataset;
        this.joursFeriesDataset = joursFeriesDataset;
    }

    public List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est invalide");
        }
        if (ChronoUnit.DAYS.between(dateDebut, dateFin) > 370) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est limitée à un an");
        }

        return seanceRepository
                .findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
                        dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay())
                .stream()
                .map(SeanceCalendrierResponse::from)
                .toList();
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
        validateSchedule(activite);
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

    /**
     * Adds the requested number of weekly sessions from a date, skipping unavailable
     * and already scheduled dates. Unlike {@link #fillSeances(Activite, int, String, LocalDate)},
     * the number represents sessions rather than a number of calendar weeks.
     */
    public List<Seance> addSeances(Activite activite, int nbSeances, LocalDate from) {
        return addSeances(activite, nbSeances, zone, from);
    }

    public List<Seance> addSeances(Activite activite, int nbSeances, String schoolZone, LocalDate from) {
        validateSchedule(activite);
        if (from == null) {
            throw new IllegalArgumentException("La date de début est obligatoire");
        }
        if (nbSeances <= 0) {
            throw new IllegalArgumentException("Le nombre de séances doit être supérieur à zéro");
        }

        LocalDate candidate = from.with(TemporalAdjusters.nextOrSame(activite.getJour()));
        LocalDate unavailableThrough = candidate.plusWeeks(nbSeances + 52L);
        Set<LocalDate> unavailableDates = getUnavailableDates(schoolZone, candidate, unavailableThrough);
        Set<LocalDate> existingDates = activite.getSeances().stream()
                .filter(seance -> seance.getDebut() != null)
                .map(seance -> seance.getDebut().toLocalDate())
                .collect(Collectors.toSet());
        List<Seance> created = new ArrayList<>();

        while (created.size() < nbSeances) {
            if (candidate.isAfter(unavailableThrough)) {
                LocalDate nextRangeStart = unavailableThrough.plusDays(1);
                unavailableThrough = unavailableThrough.plusWeeks(nbSeances + 52L);
                unavailableDates.addAll(getUnavailableDates(schoolZone, nextRangeStart, unavailableThrough));
            }

            if (!unavailableDates.contains(candidate) && !existingDates.contains(candidate)) {
                Seance seance = addFirstSeance(activite, candidate);
                activite.getSeances().add(seance);
                created.add(seance);
                existingDates.add(candidate);
            }
            candidate = candidate.plusWeeks(1);
        }

        return created;
    }

    private void validateSchedule(Activite activite) {
        if (activite.getJour() == null || activite.getHoraireDebut() == null || activite.getDuree() == null) {
            throw new IllegalArgumentException("Le jour, l'heure de début et la durée de l'activité sont obligatoires");
        }
        if (activite.getDuree() <= 0) {
            throw new IllegalArgumentException("La durée de l'activité doit être supérieure à zéro");
        }
    }

    private Set<LocalDate> getUnavailableDates(String schoolZone, LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        String vacationWhere = "zones=\"Zone " + schoolZone.toUpperCase() + "\" and start_date <= date'" + end
                + "' and end_date >= date'" + start + "'";
        Records vacations = response(vacancesApi, vacancesDataset, vacationWhere);
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

        Records holidays = response(joursFeriesApi, joursFeriesDataset,
                "statut=\"férié\" and date >= date'" + start + "' and date <= date'" + end + "'");
        holidays.getResults().stream().map(record -> date(record, "date")).filter(java.util.Objects::nonNull)
                .forEach(dates::add);
        return dates;
    }

    private Records response(DatasetApi datasetApi, String dataset, String where) {
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

    public Seance updateSeance(Long activiteId, Long seanceId, ESeance etatSeance,
                               String commentaire, boolean commentairePresent,
                               LocalDate date, LocalTime heureDebut, boolean horairePresent) {
        if (etatSeance == null && !commentairePresent && !horairePresent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune modification demandée");
        }
        if (etatSeance != null) {
            ensureUpdated(seanceRepository.updateEtat(seanceId, activiteId, etatSeance));
        }
        if (commentairePresent) {
            String normalizedComment = commentaire == null || commentaire.isBlank() ? null : commentaire.trim();
            ensureUpdated(seanceRepository.updateCommentaire(seanceId, activiteId, normalizedComment));
        }
        if (horairePresent) {
            updateHoraire(activiteId, seanceId, date, heureDebut);
        }
        return getSeance(activiteId, seanceId);
    }

    private void updateHoraire(Long activiteId, Long seanceId, LocalDate date, LocalTime heureDebut) {
        if (date == null || heureDebut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date et l'heure sont obligatoires");
        }
        Seance seance = getSeance(activiteId, seanceId);
        if (seance.getDebut() == null || seance.getFin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les horaires actuels de la séance sont incomplets");
        }
        Duration duration = Duration.between(seance.getDebut(), seance.getFin());
        if (duration.isNegative() || duration.isZero()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La durée actuelle de la séance est invalide");
        }
        LocalDateTime nouveauDebut = LocalDateTime.of(date, heureDebut);
        ensureUpdated(seanceRepository.updateHoraire(
                seanceId, activiteId, nouveauDebut, nouveauDebut.plus(duration)));
    }

    public void deleteSeance(Long activiteId, Long seanceId) {
        seanceRepository.delete(getSeance(activiteId, seanceId));
    }

    private Seance getSeance(Long activiteId, Long seanceId) {
        return seanceRepository.findByIdAndActivite_Id(seanceId, activiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable"));
    }

    private void ensureUpdated(int updatedRows) {
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable");
        }
    }

    public void modifyDay(Activite activite){
        List<Seance> seances  = activite.getSeances().stream().filter(seance -> seance.getEtatSeance().equals(ESeance.PROGRAMMEE)).toList();
        int nbSeancesRestantes = seances.size();
        seances.forEach(seanceRepository::delete);
        activite.getSeances().removeAll(seances);
        fillSeances(activite, nbSeancesRestantes);
    }
}
