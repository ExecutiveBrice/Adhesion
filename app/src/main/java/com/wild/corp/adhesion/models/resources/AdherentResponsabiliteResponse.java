package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.Adherent;

public record AdherentResponsabiliteResponse(Long id, String nom, String prenom) {
    public static AdherentResponsabiliteResponse from(Adherent adherent) {
        return new AdherentResponsabiliteResponse(adherent.getId(), adherent.getNom(), adherent.getPrenom());
    }
}
