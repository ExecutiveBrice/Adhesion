package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Presence;

public record PresencePrevueResponse(Long id, Long adherentId, String nom, String prenom,
                                    Boolean presencePrevue) {
    public static PresencePrevueResponse from(Presence presence) {
        var adherent = presence.getAdhesion().getAdherent();
        return new PresencePrevueResponse(presence.getId(), adherent.getId(), adherent.getNom(),
                adherent.getPrenom(), presence.getPresencePrevue());
    }
}
