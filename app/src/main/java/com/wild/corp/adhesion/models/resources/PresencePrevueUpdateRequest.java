package com.wild.corp.adhesion.models.resources;

import jakarta.validation.constraints.NotNull;

public record PresencePrevueUpdateRequest(@NotNull Boolean presencePrevue) {
}
