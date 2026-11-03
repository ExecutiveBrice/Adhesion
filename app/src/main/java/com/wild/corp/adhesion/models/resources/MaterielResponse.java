package com.wild.corp.adhesion.models.resources;

import java.time.OffsetDateTime;

public record MaterielResponse(Long id, String nom, ZonageConfiguration zonage,
        Long emprunteurId, String emprunteurNom, OffsetDateTime dateEmprunt, boolean horsService, String probleme,
        int quantiteTotale, int quantiteHorsService, int quantiteEmpruntee, int quantiteDisponible,
        java.util.List<EmpruntMaterielResponse> emprunts) {}
