package com.wild.corp.adhesion.security.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SwitchMemberRequest(@NotNull @Positive Long adherentId, boolean rememberSession) {}
