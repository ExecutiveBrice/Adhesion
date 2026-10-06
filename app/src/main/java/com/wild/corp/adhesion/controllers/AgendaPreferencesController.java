package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.resources.AgendaPreferences;
import com.wild.corp.adhesion.services.AgendaPreferencesServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/adherent/me/agenda-preferences")
@RequiredArgsConstructor
public class AgendaPreferencesController {
    private final AgendaPreferencesServices preferences;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public AgendaPreferences get(Authentication authentication) {
        return preferences.get(authentication.getName());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public AgendaPreferences update(Authentication authentication, @Valid @RequestBody AgendaPreferences selection) {
        return preferences.update(authentication.getName(), selection);
    }
}
