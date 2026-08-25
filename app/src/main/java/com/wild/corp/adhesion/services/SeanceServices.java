package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceCalendrierResponse;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.repository.SalleRepository;
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
import com.wild.corp.adhesion.utils.Status;

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
import java.util.UUID;


@Service
@Transactional
public class SeanceServices {

    @Autowired
    SeanceRepository seanceRepository;

    @Autowired
    SalleRepository salleRepository;

    @Autowired
    PresenceServices presenceServices;

    private final DatasetApi vacancesApi;
    private final DatasetApi joursFeriesApi;
    private final String zone;
    private final String vacancesDataset;
    private final String joursFeriesDataset;

    public SeanceServices(@Qualifier("vacancesDatasetApi") DatasetApi vacancesApi,
                          @Qualifier("joursFeriesDatasetApi") DatasetApi joursFeriesApi,
                          @Value("${adhesion.calendrier.zone:B}") String zone,
                          @Value("${adhesion.calendrier.vacances-dataset:fr-en-calendrier-scolaire}") String vacancesDataset,
                          @Value("${adhesion.calendrier.jours-feries-dataset:jours-ouvres-week-end-feries-france-2010-a-2030}") String joursFeriesDataset) {
        this.vacancesApi = vacancesApi;
        this.joursFeriesApi = joursFeriesApi;
        this.zone = zone;
        this.vacancesDataset = vacancesDataset;
        this.joursFeriesDataset = joursFeriesDataset;
    }

    public List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin) {
        return getCalendrier(dateDebut, dateFin, null);
    }

    public List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin, UUID tribuUuid) {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est invalide");
        }
        if (ChronoUnit.DAYS.between(dateDebut, dateFin) > 370) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est limitée à un an");
        }

        List<Seance> seances = tribuUuid == null
                ? seanceRepository.findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay())
                : seanceRepository.findAllByTribuAndStatutNonExcluAndDebutBetweenOrderByDebut(
                    tribuUuid,
                    List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label),
                    dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay());
        return seances
                .stream()
                .map(SeanceCalendrierResponse::from)
                .toList();
    }

    public int realiserSeancesDu(LocalDate date) {
        LocalDateTime debut = date.atStartOfDay();
        return seanceRepository.updateEtatForDebutBetweenAndEtatIn(
                debut,
                date.plusDays(1).atStartOfDay(),
                List.of(ESeance.PROGRAMMEE, ESeance.MODIFIEE),
                ESeance.REALISEE);
    }

    public Seance addFirstSeance(Activite activite, LocalDate date) {
        return addFirstSeance(activite, date, planificationHistorique(activite));
    }

    private Seance addFirstSeance(Activite activite, LocalDate date, PlanificationHebdomadaire planification) {
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setActivite(activite);
        nouvelleSeance.setSalle(planification.getSalle());
        nouvelleSeance.setDescriptif(planification.getDescriptif());
        nouvelleSeance.setDebut(LocalDateTime.of(date, planification.getHoraireDebut()));
        nouvelleSeance.setFin(nouvelleSeance.getDebut().plusMinutes(planification.getDuree()));
        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);
        return nouvelleSeance;
    }

    public void fillSeances(Activite activite, int nbWeeks) {
        fillSeances(activite, nbWeeks, zone, LocalDate.now());
    }

    /** Creates each planned weekly session, except during holidays for the requested school zone. */
    public void fillSeances(Activite activite, int nbWeeks, String schoolZone, LocalDate from) {
        List<PlanificationHebdomadaire> planifications = planifications(activite);
        validateSchedules(planifications);
        if (nbWeeks < 0) {
            throw new IllegalArgumentException("Le nombre de semaines ne peut pas être négatif");
        }
        if (nbWeeks == 0) {
            return;
        }

        LocalDate firstDate = planifications.stream()
                .map(planification -> from.with(TemporalAdjusters.nextOrSame(planification.getJour())))
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate lastDate = planifications.stream()
                .map(planification -> from.with(TemporalAdjusters.nextOrSame(planification.getJour()))
                        .plusWeeks(Math.max(0, nbWeeks - 1L)))
                .max(LocalDate::compareTo)
                .orElseThrow();
        Set<LocalDate> unavailableDates = getUnavailableDates(schoolZone, firstDate, lastDate);
        Set<LocalDateTime> existingStarts = startsExistants(activite);

        for (PlanificationHebdomadaire planification : planifications) {
            LocalDate date = from.with(TemporalAdjusters.nextOrSame(planification.getJour()));
            for (int i = 0; i < nbWeeks; i++) {
                LocalDateTime debut = LocalDateTime.of(date, planification.getHoraireDebut());
                if (!unavailableDates.contains(date) && existingStarts.add(debut)) {
                    addSeanceWithPresences(activite, date, planification);
                }
                date = date.plusWeeks(1);
            }
        }
    }

    /**
     * Adds the requested number of occurrences for every weekly schedule from a date,
     * skipping unavailable and already scheduled sessions.
     */
    public List<Seance> addSeances(Activite activite, int nbSeances, LocalDate from) {
        return addSeances(activite, nbSeances, zone, from);
    }

    public List<Seance> addSeances(Activite activite, int nbSeances, String schoolZone, LocalDate from) {
        return addSeances(activite, planifications(activite), nbSeances, schoolZone, from);
    }

    public List<Seance> addSeances(Activite activite, PlanificationHebdomadaire planification,
                                    int nbSeances, LocalDate from) {
        return addSeances(activite, List.of(planification), nbSeances, zone, from);
    }

    private List<Seance> addSeances(Activite activite, List<PlanificationHebdomadaire> planifications,
                                    int nbSeances, String schoolZone, LocalDate from) {
        validateSchedules(planifications);
        if (from == null) {
            throw new IllegalArgumentException("La date de début est obligatoire");
        }
        if (nbSeances <= 0) {
            throw new IllegalArgumentException("Le nombre de séances doit être supérieur à zéro");
        }

        LocalDate earliestCandidate = planifications.stream()
                .map(planification -> from.with(TemporalAdjusters.nextOrSame(planification.getJour())))
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate unavailableThrough = earliestCandidate.plusWeeks(nbSeances + 52L);
        Set<LocalDate> unavailableDates = getUnavailableDates(schoolZone, earliestCandidate, unavailableThrough);
        Set<LocalDateTime> existingStarts = startsExistants(activite);
        List<Seance> created = new ArrayList<>();

        for (PlanificationHebdomadaire planification : planifications) {
            LocalDate candidate = from.with(TemporalAdjusters.nextOrSame(planification.getJour()));
            int createdForPlanification = 0;
            while (createdForPlanification < nbSeances) {
                if (candidate.isAfter(unavailableThrough)) {
                    LocalDate nextRangeStart = unavailableThrough.plusDays(1);
                    unavailableThrough = unavailableThrough.plusWeeks(nbSeances + 52L);
                    unavailableDates.addAll(getUnavailableDates(schoolZone, nextRangeStart, unavailableThrough));
                }

                LocalDateTime debut = LocalDateTime.of(candidate, planification.getHoraireDebut());
                if (!unavailableDates.contains(candidate) && existingStarts.add(debut)) {
                    created.add(addSeanceWithPresences(activite, candidate, planification));
                    createdForPlanification++;
                }
                candidate = candidate.plusWeeks(1);
            }
        }

        return created;
    }

    private void validateSchedules(List<PlanificationHebdomadaire> planifications) {
        if (planifications.isEmpty()) {
            throw new IllegalArgumentException("Au moins une séance hebdomadaire est obligatoire");
        }
        for (PlanificationHebdomadaire planification : planifications) {
            if (planification.getJour() == null || planification.getHoraireDebut() == null
                    || planification.getDuree() == null) {
                throw new IllegalArgumentException("Le jour, l'heure de début et la durée de chaque séance sont obligatoires");
            }
            if (planification.getDuree() <= 0) {
                throw new IllegalArgumentException("La durée de chaque séance doit être supérieure à zéro");
            }
        }
        long distinctSlots = planifications.stream()
                .map(planification -> planification.getJour() + "-" + planification.getHoraireDebut())
                .distinct()
                .count();
        if (distinctSlots != planifications.size()) {
            throw new IllegalArgumentException("Deux séances hebdomadaires ne peuvent pas avoir le même jour et le même horaire");
        }
    }

    private List<PlanificationHebdomadaire> planifications(Activite activite) {
        if (activite.getPlanificationsHebdomadaires() != null && !activite.getPlanificationsHebdomadaires().isEmpty()) {
            return activite.getPlanificationsHebdomadaires();
        }
        return List.of(planificationHistorique(activite));
    }

    private PlanificationHebdomadaire planificationHistorique(Activite activite) {
        PlanificationHebdomadaire planification = new PlanificationHebdomadaire();
        planification.setJour(activite.getJour());
        planification.setHoraireDebut(activite.getHoraireDebut());
        planification.setDuree(activite.getDuree());
        planification.setSalle(activite.getSalle());
        return planification;
    }

    private Set<LocalDateTime> startsExistants(Activite activite) {
        return activite.getSeances().stream()
                .map(Seance::getDebut)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Seance addSeanceWithPresences(Activite activite, LocalDate date,
                                          PlanificationHebdomadaire planification) {
        Seance seance = addFirstSeance(activite, date, planification);
        activite.getSeances().add(seance);
        presenceServices.fillPresences(seance);
        return seance;
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
                               LocalDate date, LocalTime heureDebut, boolean horairePresent,
                               Long salleId, boolean sallePresente) {
        if (etatSeance == null && !commentairePresent && !horairePresent && !sallePresente) {
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
        if (sallePresente) {
            getSeance(activiteId, seanceId).setSalle(trouverSalle(salleId));
        }
        return getSeance(activiteId, seanceId);
    }

    private Salle trouverSalle(Long salleId) {
        if (salleId == null) {
            return null;
        }
        return salleRepository.findById(salleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La salle sélectionnée n'existe plus"));
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

    public Seance updateCommentaireForManager(Long seanceId, String commentaire, String username) {
        Seance seance = seanceRepository.findByIdAndManagerUsername(seanceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable"));
        seance.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        return seance;
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
