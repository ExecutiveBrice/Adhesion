package com.wild.corp.adhesion.models.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AjoutAdherentSeanceRequest(
        @NotBlank(message = "L’adresse e-mail est obligatoire")
        @Email(message = "L’adresse e-mail est invalide")
        String email
) {
}
