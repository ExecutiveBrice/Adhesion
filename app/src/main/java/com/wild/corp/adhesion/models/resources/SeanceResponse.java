package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.*;

import java.time.LocalDateTime;

public record SeanceResponse(
        Long id,
        ESeance etatSeance,
        String causeAnnulation,
        LocalDateTime debut,
        LocalDateTime fin,
        String commentaire,
        Salle salle,
        Presence presence,
        Activite activite
) {
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
                seance.getActivite()
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
                seance.getActivite()
        );
    }
}
