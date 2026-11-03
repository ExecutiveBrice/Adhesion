package com.wild.corp.adhesion.models.resources;
public record MaterielRequest(String nom, Long zonageId, Boolean horsService, Integer quantiteTotale) {
    public MaterielRequest(String nom, Long zonageId) { this(nom, zonageId, null, null); }
    public MaterielRequest(String nom, Long zonageId, Boolean horsService) { this(nom, zonageId, horsService, null); }
}
