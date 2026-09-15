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
                nomActivite(seance),
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

    private static String nomActivite(Seance seance) {
        String descriptif = seance.getActivite().getPlanificationsHebdomadaires().stream()
                .filter(planification -> planification.getJour() != null && planification.getHoraireDebut() != null)
                .filter(planification -> seance.getDebut() != null
                        && planification.getJour().equals(seance.getDebut().getDayOfWeek())
                        && planification.getHoraireDebut().equals(seance.getDebut().toLocalTime()))
                .map(planification -> planification.getDescriptif())
                .filter(valeur -> valeur != null && !valeur.isBlank())
                .findFirst()
                .orElse(seance.getDescriptif());
        return descriptif == null || descriptif.isBlank()
                ? seance.getActivite().getNom()
                : seance.getActivite().getNom() + " – " + descriptif.trim();
    }
}
