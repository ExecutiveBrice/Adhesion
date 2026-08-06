package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;

import java.time.LocalDateTime;

public record SeanceCalendrierResponse(
        Long id,
        Long activiteId,
        String activiteNom,
        String horaireActivite,
        String salle,
        String adresseSalle,
        String couleurSalle,
        String commentaire,
        String lien,
        LocalDateTime debut,
        LocalDateTime fin,
        ESeance etatSeance
) {
    public static SeanceCalendrierResponse from(Seance seance) {
        var activite = seance.getActivite();
        var salle = seance.getSalle();
        return new SeanceCalendrierResponse(
                seance.getId(),
                activite.getId(),
                activite.getNom(),
                activite.getHoraire(),
                salle != null ? salle.getNom() : null,
                salle != null ? salle.getAdresse() : null,
                salle != null ? salle.getCouleur() : null,
                seance.getCommentaire(),
                activite.getLien(),
                seance.getDebut(),
                seance.getFin(),
                seance.getEtatSeance()
        );
    }
}
