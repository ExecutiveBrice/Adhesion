package com.wild.corp.adhesion.models.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResponsabiliteSeanceRequest(@NotNull @Positive Long tacheId, @NotNull @Positive Long adherentId) {
}
