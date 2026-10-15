package com.wild.corp.adhesion.models.resources;
public record HorsServiceRequest(Boolean horsService, String commentaire, Integer quantiteHorsService) {
    public HorsServiceRequest(Boolean horsService, String commentaire) { this(horsService, commentaire, null); }
}
