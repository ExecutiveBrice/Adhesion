package com.wild.corp.adhesion.models.resources;

import java.util.List;

public record SectionConfiguration(Long id, String nom, String type, List<Long> referentUserIds, String couleur) {
    public SectionConfiguration(Long id, String nom, String type) {
        this(id, nom, type, null, null);
    }

    public SectionConfiguration(Long id, String nom, String type, List<Long> referentUserIds) {
        this(id, nom, type, referentUserIds, null);
    }
}
