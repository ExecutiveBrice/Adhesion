package com.wild.corp.adhesion.models.resources;

public record TacheSeanceConfiguration(Long id, String nom, String icone, String couleur) {
    public TacheSeanceConfiguration(Long id, String nom) {
        this(id, nom, null, null);
    }
}
