package com.wild.corp.adhesion.security.payload.request;

import com.wild.corp.adhesion.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Size(max = 256) String token,
        @StrongPassword String password) {
}
