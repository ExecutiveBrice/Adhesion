package com.wild.corp.adhesion.shop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CartItemRequest(
        @NotNull(message = "La variante est obligatoire") Long variantId,
        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être strictement positive") Integer quantity) {

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Le champ " + fieldName + " n'est pas autorisé");
    }
}
