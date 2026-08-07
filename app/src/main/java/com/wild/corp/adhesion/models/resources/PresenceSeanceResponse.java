package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Presence;

public record PresenceSeanceResponse(Long id, Long adherentId, String nom, String prenom, boolean presence) {
    public static PresenceSeanceResponse from(Presence presence) {
        return new PresenceSeanceResponse(
                presence.getId(),
                presence.getAdhesion().getAdherent().getId(),
                presence.getAdhesion().getAdherent().getNom(),
                presence.getAdhesion().getAdherent().getPrenom(),
                Boolean.TRUE.equals(presence.getPresence())
        );
    }
}
