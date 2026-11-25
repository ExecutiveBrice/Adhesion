package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.PresenceSeanceResponse;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class PresenceServices {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Autowired
    PresenceRepository presenceRepository;
    @Autowired
    SeanceRepository seanceRepository;


    public void fillPresences(Adhesion adhesion) {
        if (!doitAvoirDesPresences(adhesion.getStatutActuel())) {
            return;
        }

        adhesion.getActivite().getSeances().stream()
                .filter(this::estACompleter)
                .forEach(seance -> addPresence(adhesion, seance));
    }

    public void fillPresences(Seance seance) {
        if (!estACompleter(seance)) {
            return;
        }

        seance.getActivite().getAdhesions().stream()
                .filter(adhesion -> doitAvoirDesPresences(adhesion.getStatutActuel()))
                .forEach(adhesion -> addPresence(adhesion, seance));
    }

    public void supprimerPresencesAVenir(Adhesion adhesion, Long activiteConserveeId) {
        LocalDateTime maintenant = LocalDateTime.now(PARIS);
        adhesion.getPresences().removeIf(presence -> {
            Seance seance = presence.getSeance();
            if (seance == null || seance.getDebut() == null || !seance.getDebut().isAfter(maintenant)) {
                return false;
            }
            if (activiteConserveeId != null && seance.getActivite() != null
                    && activiteConserveeId.equals(seance.getActivite().getId())) {
                return false;
            }

            seance.getPresences().remove(presence);
            return true;
        });
    }

    private boolean estACompleter(Seance seance) {
        return seance.getEtatSeance() == ESeance.PROGRAMMEE || seance.getEtatSeance() == ESeance.MODIFIEE;
    }

    static boolean doitAvoirDesPresences(String statut) {
        return !Status.LISTE_ATTENTE.label.equals(statut) && !Status.ANNULEE.label.equals(statut);
    }

    private Presence addPresence(Adhesion adhesion, Seance seance) {
        for (Presence presence : seance.getPresences()) {
            if (presence.getAdhesion() == adhesion || (adhesion.getId() != null && adhesion.equals(presence.getAdhesion()))) {
                return presence;
            }
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
            presenceRepository.save(presence);
        }
        seance.getPresences().add(presence);
        return presence;
    }

    /**
     * Adds a participant directly to one session. This is used for a person
     * registered by a professor during the session, before their full file is
     * completed.
     */
    public Presence addPresenceForSeance(Adhesion adhesion, Seance seance, boolean present) {
        Presence presence = addPresence(adhesion, seance);
        presence.setPresence(present);
        presence.setDateModification(LocalDate.now());
        return presence;
    }

    public List<PresenceSeanceResponse> getPresences(Long seanceId, String username) {
        getSeanceForManager(seanceId, username);
        return getPresences(seanceId);
    }

    public List<PresenceSeanceResponse> getPresencesForSecretary(Long seanceId) {
        getSeance(seanceId);
        return getPresences(seanceId);
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
