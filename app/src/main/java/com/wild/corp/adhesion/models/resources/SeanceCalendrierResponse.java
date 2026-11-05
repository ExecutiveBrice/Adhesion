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
        String causeAnnulation,
        String nomEvenement,
        String typeEvenement,
        String adversaire
) {
    public static SeanceCalendrierResponse from(Seance seance) {
        var activite = seance.getActivite();
        var salle = seance.getSalle();
        return new SeanceCalendrierResponse(
                seance.getId(),
                activite.getId(),
                nomActivite(seance),
                activite.getHoraire(),
                salle != null ? salle.getNom() : seance.getLieuExterieur(),
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
                seance.getCauseAnnulation(),
                seance.getDescriptif(),
                seance.getTypeEvenement(),
                seance.getAdversaire()
        );
    }

    private static String nomActivite(Seance seance) {
        String descriptif = descriptifSeance(seance);
        return descriptif == null ? seance.getActivite().getNom()
                : seance.getActivite().getNom() + " – " + descriptif;
    }

    private static String descriptifSeance(Seance event) {
        String teams = event.getEquipes().stream().map(t -> t.getNom()).sorted().collect(java.util.stream.Collectors.joining(" / "));
        String description = event.getDescriptif();
        return teams.isBlank() ? (description == null || description.isBlank() ? null : description.trim()) : teams;
    }

}
