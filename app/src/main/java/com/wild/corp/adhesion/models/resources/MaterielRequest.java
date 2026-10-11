package com.wild.corp.adhesion.models.resources;

public record MaterielRequest(String nom, Long zonageId, Boolean horsService) {
    public MaterielRequest(String nom, Long zonageId) { this(nom, zonageId, null); }
}
