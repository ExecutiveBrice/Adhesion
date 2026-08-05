package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record MiseAJourSeanceRequest(
        ESeance etatSeance,
        @Size(max = 255) String commentaire,
        boolean commentairePresent,
        LocalDate date,
        LocalTime heureDebut,
        boolean horairePresent
) {
}
