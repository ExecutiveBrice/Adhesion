package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceCalendrierResponse;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.repository.SalleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.wild.corp.adhesion.utils.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Autowired
    ResponsabiliteSeanceServices responsabiliteSeanceServices;

    public List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin) {
        return getCalendrier(dateDebut, dateFin, null);
    }

    public List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin, UUID tribuUuid) {
        return getCalendrier(dateDebut, dateFin, tribuUuid, null);
    }

    public List<SeanceCalendrierResponse> getCalendrierForAdherent(LocalDate dateDebut, LocalDate dateFin,
                                                                     Long adherentId) {
        if (adherentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adhérent est obligatoire");
        }
        return getCalendrier(dateDebut, dateFin, null, adherentId);
    }

    public List<SeanceResponse> getSeancesForAdherent(LocalDate dateDebut, LocalDate dateFin, Long adherentId) {
        if (adherentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adhérent est obligatoire");
        }
        validatePeriod(dateDebut, dateFin);

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();
        List<String> statutsExclus = List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label);
        return seanceRepository.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(
                        adherentId, statutsExclus, debut, fin)
                .stream()
                .map(seance -> SeanceResponse.from(seance, adherentId))
                .toList();
    }

    private List<SeanceCalendrierResponse> getCalendrier(LocalDate dateDebut, LocalDate dateFin, UUID tribuUuid,
                                                          Long adherentId) {
        validatePeriod(dateDebut, dateFin);

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();
        List<String> statutsExclus = List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label);
        List<Seance> seances = adherentId != null
                ? seanceRepository.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(
                    adherentId, statutsExclus, debut, fin)
                : tribuUuid == null
                    ? seanceRepository.findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(debut, fin)
                    : seanceRepository.findAllByTribuAndStatutNonExcluAndDebutBetweenOrderByDebut(
                    tribuUuid,
                    statutsExclus, debut, fin);
        return seances
                .stream()
                .map(SeanceCalendrierResponse::from)
                .toList();
    }

    private void validatePeriod(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est invalide");
        }
        if (ChronoUnit.DAYS.between(dateDebut, dateFin) > 370) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est limitée à un an");
        }
    }

    public int realiserSeancesDu(LocalDate date) {
        LocalDateTime debut = date.atStartOfDay();
        return seanceRepository.updateEtatForDebutBetweenAndEtatIn(
                debut,
                date.plusDays(1).atStartOfDay(),
                List.of(ESeance.PROGRAMMEE, ESeance.MODIFIEE),
                ESeance.REALISEE);
    }

    public int realiserSeancesAvant(LocalDate date) {
        return seanceRepository.updateEtatForDebutBeforeAndEtatIn(
                date.atStartOfDay(),
                List.of(ESeance.PROGRAMMEE, ESeance.MODIFIEE),
                ESeance.REALISEE);
    }

    public Seance updateCommentaireForManager(Long seanceId, String commentaire, String username) {
        Seance seance = seanceRepository.findByIdAndManagerUsername(seanceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        seance.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        return seance;
    }

    public Seance updateCommentaireForSecretary(Long seanceId, String commentaire) {
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        seance.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        return seance;
    }

}
