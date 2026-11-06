package com.wild.corp.adhesion.models.resources;

public record EmpruntCalendrierMaterielResponse(Long materielId, String materielNom,
        ZonageConfiguration zonage, EmpruntMaterielResponse emprunt) {}
