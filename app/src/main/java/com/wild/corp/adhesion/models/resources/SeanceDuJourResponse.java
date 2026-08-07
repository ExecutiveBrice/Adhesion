package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.ESeance;

import java.time.LocalDateTime;

public record SeanceDuJourResponse(
        Long id,
        String activite,
        LocalDateTime debut,
        LocalDateTime fin,
        String lieu,
        String adresse,
        String commentaire,
        ESeance etatSeance,
        long nombreParticipants
) {
    public static SeanceDuJourResponse from(Seance seance) {
        return new SeanceDuJourResponse(
                seance.getId(),
                seance.getActivite().getNom(),
                seance.getDebut(),
                seance.getFin(),
                seance.getSalle() == null ? seance.getActivite().getNomSalle() : seance.getSalle().getNom(),
                seance.getSalle() == null ? null : seance.getSalle().getAdresse(),
                seance.getCommentaire(),
                seance.getEtatSeance(),
                seance.getActivite().getAdhesions().stream().filter(adhesion -> adhesion.isValide()).count()
        );
    }
}
