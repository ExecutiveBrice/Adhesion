package com.wild.corp.adhesion.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Le lien de réinitialisation est invalide ou expiré")
public class InvalidConfirmationTokenException extends RuntimeException {
    public InvalidConfirmationTokenException() {
        super("Le lien de réinitialisation est invalide ou expiré");
    }
}
