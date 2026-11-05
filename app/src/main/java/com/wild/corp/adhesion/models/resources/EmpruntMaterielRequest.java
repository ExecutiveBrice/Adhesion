package com.wild.corp.adhesion.models.resources;

import java.time.LocalDate;

public record EmpruntMaterielRequest(Long emprunteurId, Integer quantite, LocalDate dateDebut, LocalDate dateFinPrevue) {
    public EmpruntMaterielRequest(Long emprunteurId, Integer quantite) { this(emprunteurId, quantite, null, null); }
    public EmpruntMaterielRequest(Long emprunteurId) { this(emprunteurId, null); }
}
