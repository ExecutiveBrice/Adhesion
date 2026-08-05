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
        LocalDateTime debut,
        LocalDateTime fin,
        ESeance etatSeance
) {
    public static SeanceCalendrierResponse from(Seance seance) {
        return new SeanceCalendrierResponse(
                seance.getId(),
                seance.getActivite().getId(),
                seance.getActivite().getNom(),
                seance.getActivite().getHoraire(),
                seance.getActivite().getSalle(),
                seance.getDebut(),
                seance.getFin(),
                seance.getEtatSeance()
        );
    }
}
