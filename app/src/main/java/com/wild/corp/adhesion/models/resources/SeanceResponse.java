package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;

import java.time.LocalDateTime;

public record SeanceResponse(
        Long id,
        ESeance etatSeance,
        String causeAnnulation,
        LocalDateTime debut,
        LocalDateTime fin,
        String commentaire
) {
    public static SeanceResponse from(Seance seance) {
        return new SeanceResponse(
                seance.getId(),
                seance.getEtatSeance(),
                seance.getCauseAnnulation(),
                seance.getDebut(),
                seance.getFin(),
                seance.getCommentaire()
        );
    }
}
