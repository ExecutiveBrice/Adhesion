package com.wild.corp.adhesion.models.resources;

import java.time.OffsetDateTime;

public record EmpruntMaterielResponse(Long id, Long emprunteurId, String emprunteurNom,
        OffsetDateTime dateEmprunt, OffsetDateTime dateRetour, String enregistreParNom, String rapporteParNom,
        Boolean horsService, String commentaire) {}
