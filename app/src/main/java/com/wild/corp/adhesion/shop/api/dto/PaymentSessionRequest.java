package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record PaymentSessionRequest(@NotNull URI returnUrl, @NotNull URI cancelUrl) {
}
