package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.PresenceSeanceResponse;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PresenceServices {

    @Autowired
    PresenceRepository presenceRepository;
    @Autowired
    SeanceRepository seanceRepository;


    public void fillPresences(Adhesion adhesion) {
        adhesion.getActivite().getSeances().stream()
                .filter(seance -> seance.getEtatSeance() == ESeance.PROGRAMMEE)
                .forEach(seance -> addPresence(adhesion, seance));
    }

    public void fillPresences(Seance seance) {
        seance.getActivite().getAdhesions().stream()
                .filter(Adhesion::isValide)
                .forEach(adhesion -> addPresence(adhesion, seance));
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

    public List<PresenceSeanceResponse> getPresences(Long seanceId, String username) {
        getSeanceForManager(seanceId, username);
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

    private Seance getSeanceForManager(Long seanceId, String username) {
        return seanceRepository.findByIdAndManagerUsername(seanceId, username)
                .orElseThrow(() -> new IllegalArgumentException("Séance introuvable"));
    }

}
