package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.PresenceSeanceResponse;
import com.wild.corp.adhesion.models.resources.PresencePrevueResponse;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PresenceServices {

    @Autowired
    PresenceRepository presenceRepository;
    @Autowired
    SeanceRepository seanceRepository;


    public void fillPresences(Adhesion adhesion) {
        if (!doitAvoirDesPresences(adhesion)) {
            return;
        }

        adhesion.getActivite().getSeances().stream()
                .filter(seance -> seance.getEtatSeance() == ESeance.PROGRAMMEE)
                .forEach(seance -> addPresence(adhesion, seance));
    }

    public void fillPresences(Seance seance) {
        seance.getActivite().getAdhesions().stream()
                .filter(this::doitAvoirDesPresences)
                .forEach(adhesion -> addPresence(adhesion, seance));
    }

    private boolean doitAvoirDesPresences(Adhesion adhesion) {
        return !Status.LISTE_ATTENTE.label.equals(adhesion.getStatutActuel())
                && !Status.ANNULEE.label.equals(adhesion.getStatutActuel());
    }

    private void addPresence(Adhesion adhesion, Seance seance) {
        boolean alreadyExists = adhesion.getPresences().stream()
                .anyMatch(presence -> presence.getSeance() == seance
                        || (presence.getSeance() != null && presence.getSeance().equals(seance)))
                || seance.getPresences().stream()
                .anyMatch(presence -> presence.getAdhesion() == adhesion
                        || (presence.getAdhesion() != null && presence.getAdhesion().equals(adhesion)));
        if (alreadyExists) {
            return;
        }

        Presence presence = new Presence();
        presence.setAdhesion(adhesion);
        presence.setSeance(seance);
        // Une séance nouvellement planifiée n'est pas encore persistée. L'ajouter
        // à une adhésion déjà gérée déclencherait une cascade vers cette séance
        // transitoire. La cascade de la séance persiste alors la présence ; le
        // lien sera ensuite visible via la relation de l'adhésion.
        if (seance.getId() != null) {
            adhesion.getPresences().add(presence);
        }
        seance.getPresences().add(presence);
    }

    /**
     * Adds a participant directly to one session. This is used for a person
     * registered by a professor during the session, before their full file is
     * completed.
     */
    public Presence addPresenceForSeance(Adhesion adhesion, Seance seance, boolean present) {
        Presence presence = new Presence();
        presence.setAdhesion(adhesion);
        presence.setSeance(seance);
        presence.setPresence(present);
        presence.setDateModification(LocalDate.now());
        return presenceRepository.save(presence);
    }

    public List<PresenceSeanceResponse> getPresences(Long seanceId, String username) {
        getSeanceForManager(seanceId, username);
        return getPresences(seanceId);
    }

    public List<PresenceSeanceResponse> getPresencesForSecretary(Long seanceId) {
        getSeance(seanceId);
        return getPresences(seanceId);
    }

    public List<PresencePrevueResponse> getPresencesPrevues(Long seanceId, String username) {
        getPresenceForAdherent(seanceId, username);
        return presenceRepository.findBySeance_IdOrderByAdhesion_Adherent_NomAscAdhesion_Adherent_PrenomAsc(seanceId)
                .stream()
                .filter(presence -> doitAvoirDesPresences(presence.getAdhesion()))
                .map(PresencePrevueResponse::from).toList();
    }

    public PresencePrevueResponse updatePresencePrevue(Long seanceId, boolean presencePrevue, String username) {
        Presence presence = getPresenceForAdherent(seanceId, username);
        Seance seance = presence.getSeance();
        if ((seance.getEtatSeance() != ESeance.PROGRAMMEE && seance.getEtatSeance() != ESeance.MODIFIEE)
                || seance.getDebut() == null || !seance.getDebut().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La présence prévue ne peut être renseignée que pour une séance à venir");
        }
        presence.setPresencePrevue(presencePrevue);
        presence.setDateModification(LocalDate.now());
        return PresencePrevueResponse.from(presence);
    }

    private Presence getPresenceForAdherent(Long seanceId, String username) {
        Presence presence = presenceRepository.findBySeance_IdAndAdhesion_Adherent_User_Username(seanceId, username)
                .filter(value -> doitAvoirDesPresences(value.getAdhesion()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription à la séance introuvable"));
        if (!"COMPETITION".equals(presence.getSeance().getActivite().getGroupe())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Les présences prévues sont réservées aux sections compétitives");
        }
        return presence;
    }

    private List<PresenceSeanceResponse> getPresences(Long seanceId) {
        return presenceRepository.findBySeance_IdOrderByAdhesion_Adherent_NomAscAdhesion_Adherent_PrenomAsc(seanceId)
                .stream().map(PresenceSeanceResponse::from).toList();
    }

    public PresenceSeanceResponse updatePresence(Long seanceId, Long presenceId, boolean present, String username) {
        getSeanceForManager(seanceId, username);
        Presence presence = presenceRepository.findById(presenceId)
                .filter(value -> value.getSeance() != null && seanceId.equals(value.getSeance().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Présence introuvable"));
        presence.setPresence(present);
        presence.setDateModification(LocalDate.now());
        return PresenceSeanceResponse.from(presence);
    }

    public PresenceSeanceResponse updatePresenceForSecretary(Long seanceId, Long presenceId, boolean present) {
        getSeance(seanceId);
        Presence presence = presenceRepository.findById(presenceId)
                .filter(value -> value.getSeance() != null && seanceId.equals(value.getSeance().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Présence introuvable"));
        presence.setPresence(present);
        presence.setDateModification(LocalDate.now());
        return PresenceSeanceResponse.from(presence);
    }

    private Seance getSeanceForManager(Long seanceId, String username) {
        return seanceRepository.findByIdAndManagerUsername(seanceId, username)
                .orElseThrow(() -> new IllegalArgumentException("Séance introuvable"));
    }

    private Seance getSeance(Long seanceId) {
        return seanceRepository.findById(seanceId)
                .orElseThrow(() -> new IllegalArgumentException("Séance introuvable"));
    }

}
