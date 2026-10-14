package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.AdherentNewsletterService;
import com.wild.corp.adhesion.services.NewsletterApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/adherent/{id}/newsletter")
@RequiredArgsConstructor
public class AdherentNewsletterController {
    private final AdherentNewsletterService service;
    public record Preference(@NotNull Boolean subscribed, @NotBlank String expectedEmail) { }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public AdherentNewsletterService.Subscription status(@PathVariable long id, Authentication principal) {
        return service.status(id, principal);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public AdherentNewsletterService.Subscription update(@PathVariable long id, Authentication principal,
                                                         @Valid @RequestBody Preference preference) {
        return service.update(id, principal, preference.subscribed(), preference.expectedEmail());
    }

    @ExceptionHandler(NewsletterApiException.class)
    public ResponseEntity<ProblemDetail> brevoError(NewsletterApiException exception) {
        HttpStatus status = exception.getStatusCode() == 429 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        var response = ResponseEntity.status(status);
        if (exception.getStatusCode() == 429) response.header("Retry-After", String.valueOf(Math.max(1, exception.getRetryAfter().toSeconds())));
        return response.body(ProblemDetail.forStatusAndDetail(status,
                "Le statut newsletter n’a pas pu être confirmé auprès de Brevo. Actualisez le statut avant de réessayer."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail error(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail configurationError() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "La newsletter est indisponible. Contactez le secrétariat.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidEmail() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "L’adresse e-mail enregistrée n’est pas valide.");
    }
}
