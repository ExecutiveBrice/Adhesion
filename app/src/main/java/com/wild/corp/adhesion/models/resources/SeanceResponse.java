package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.*;

import java.time.LocalDateTime;
import java.util.List;

public record SeanceResponse(
        Long id,
        ESeance etatSeance,
        String causeAnnulation,
        LocalDateTime debut,
        LocalDateTime fin,
        String commentaire,
        Salle salle,
        Presence presence,
        Activite activite,
        List<ResponsabiliteSeance> responsabilites
) {
    public SeanceResponse(Long id, ESeance etatSeance, String causeAnnulation, LocalDateTime debut,
                          LocalDateTime fin, String commentaire, Salle salle, Presence presence, Activite activite) {
        this(id, etatSeance, causeAnnulation, debut, fin, commentaire, salle, presence, activite, List.of());
    }

    public static SeanceResponse from(Seance seance) {
        return new SeanceResponse(
                seance.getId(),
                seance.getEtatSeance(),
                seance.getCauseAnnulation(),
                seance.getDebut(),
                seance.getFin(),
                seance.getCommentaire(),
                seance.getSalle(),
                null,
                // Activity management already knows the activity. Repeating its full entity for every
                // session serializes all schedules and participants once per row.
                null,
                List.copyOf(seance.getResponsabilites())
        );
    }

    /**
     * Maps a session for one member and includes their own attendance record.
     * Other members' attendance records are deliberately not exposed.
     */
    public static SeanceResponse from(Seance seance, Long adherentId) {
        Presence presence = seance.getPresences().stream()
                .filter(value -> value.getAdhesion() != null
                        && value.getAdhesion().getAdherent() != null
                        && adherentId.equals(value.getAdhesion().getAdherent().getId()))
                .findFirst()
                .orElse(null);
        return new SeanceResponse(
                seance.getId(),
                seance.getEtatSeance(),
                seance.getCauseAnnulation(),
                seance.getDebut(),
                seance.getFin(),
                seance.getCommentaire(),
                seance.getSalle(),
                presence,
                seance.getActivite(),
                List.copyOf(seance.getResponsabilites())
        );
    }
}
