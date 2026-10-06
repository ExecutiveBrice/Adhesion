package com.wild.corp.adhesion.models.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AgendaPreferences(
        @NotNull @Size(max = 1000) Set<@NotNull @Positive Long> sectionsMasquees,
        @NotNull @Size(max = 1000) Set<@NotNull @Positive Long> agendasGoogleMasques
) { }
