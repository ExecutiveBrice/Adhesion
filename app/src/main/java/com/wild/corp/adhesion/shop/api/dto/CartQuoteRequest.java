package com.wild.corp.adhesion.shop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CartQuoteRequest(
        @NotEmpty(message = "Le panier doit contenir au moins un article")
        @Size(max = 100, message = "Le panier ne peut pas contenir plus de 100 lignes")
        List<@Valid CartItemRequest> items) {

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Le champ " + fieldName + " n'est pas autorisé");
    }
}
