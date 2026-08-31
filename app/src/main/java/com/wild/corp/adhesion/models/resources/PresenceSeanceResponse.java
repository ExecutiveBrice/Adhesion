package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Presence;

public record PresenceSeanceResponse(Long id, Long adherentId, String nom, String prenom, String email, boolean presence,
                                    boolean paiementValide, boolean documentsValides, String statutAdhesion) {
    public static PresenceSeanceResponse from(Presence presence) {
        return new PresenceSeanceResponse(
                presence.getId(),
                presence.getAdhesion().getAdherent().getId(),
                presence.getAdhesion().getAdherent().getNom(),
                presence.getAdhesion().getAdherent().getPrenom(),
                presence.getAdhesion().getAdherent().getUser() == null
                        ? null : presence.getAdhesion().getAdherent().getUser().getUsername(),
                Boolean.TRUE.equals(presence.getPresence()),
                Boolean.TRUE.equals(presence.getAdhesion().getValidPaiementSecretariat()),
                Boolean.TRUE.equals(presence.getAdhesion().getValidDocumentSecretariat()),
                presence.getAdhesion().getStatutActuel()
        );
    }
}
