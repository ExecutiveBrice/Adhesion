package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record MiseAJourSeanceRequest(
        ESeance etatSeance,
        @Size(max = 255) String commentaire,
        Boolean commentairePresent,
        LocalDate date,
        LocalTime heureDebut,
        Boolean horairePresent,
        Long salleId,
        Boolean sallePresente
) {
}
