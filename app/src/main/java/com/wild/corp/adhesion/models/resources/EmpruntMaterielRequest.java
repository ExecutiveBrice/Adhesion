package com.wild.corp.adhesion.models.resources;
public record EmpruntMaterielRequest(Long emprunteurId, Integer quantite) {
    public EmpruntMaterielRequest(Long emprunteurId) { this(emprunteurId, null); }
}
