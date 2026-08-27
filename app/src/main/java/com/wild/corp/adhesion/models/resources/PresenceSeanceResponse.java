package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Presence;

public record PresenceSeanceResponse(Long id, Long adherentId, String nom, String prenom, String email, boolean presence) {
    public static PresenceSeanceResponse from(Presence presence) {
        return new PresenceSeanceResponse(
                presence.getId(),
                presence.getAdhesion().getAdherent().getId(),
                presence.getAdhesion().getAdherent().getNom(),
                presence.getAdhesion().getAdherent().getPrenom(),
                presence.getAdhesion().getAdherent().getUser() == null
                        ? null : presence.getAdhesion().getAdherent().getUser().getUsername(),
                Boolean.TRUE.equals(presence.getPresence())
        );
    }
}
