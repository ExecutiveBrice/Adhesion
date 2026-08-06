package com.wild.corp.adhesion.models.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AjoutSeancesRequest(
        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,
        @NotNull(message = "Le nombre de séances est obligatoire")
        @Min(value = 1, message = "Le nombre de séances doit être supérieur à zéro")
        @Max(value = 200, message = "Le nombre de séances ne peut pas dépasser 200")
        Integer nombreSeances
) {
}
