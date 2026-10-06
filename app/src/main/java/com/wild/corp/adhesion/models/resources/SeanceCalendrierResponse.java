package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.ResponsabiliteSeance;

import java.time.LocalDateTime;
import java.util.List;

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
        ESeance etatSeance,
        String sectionType,
        Long sectionId,
        String sectionNom,
        String couleurSection,
        List<ResponsabiliteSeance> responsabilites,
        String activiteNomCourt,
        String descriptif,
        String causeAnnulation
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
                seance.getEtatSeance(),
                activite.getGroupe(),
                activite.getSection() != null ? activite.getSection().getId() : null,
                activite.getGroupeFiltre(),
                activite.getSection() != null ? activite.getSection().getCouleur() : null,
                List.copyOf(seance.getResponsabilites()),
                activite.getNom(),
                descriptifSeance(seance),
                seance.getCauseAnnulation()
        );
    }

    private static String nomActivite(Seance seance) {
        String descriptif = descriptifSeance(seance);
        return descriptif == null ? seance.getActivite().getNom()
                : seance.getActivite().getNom() + " – " + descriptif;
    }

    private static String descriptifSeance(Seance seance) {
        String descriptif = seance.getActivite().getPlanificationsHebdomadaires().stream()
                .filter(planification -> planification.getJour() != null && planification.getHoraireDebut() != null)
                .filter(planification -> seance.getDebut() != null
                        && planification.getJour().equals(seance.getDebut().getDayOfWeek())
                        && planification.getHoraireDebut().equals(seance.getDebut().toLocalTime()))
                .map(planification -> planification.getDescriptif())
                .filter(valeur -> valeur != null && !valeur.isBlank())
                .findFirst()
                .orElse(seance.getDescriptif());
        return descriptif == null || descriptif.isBlank() ? null : descriptif.trim();
    }
}
