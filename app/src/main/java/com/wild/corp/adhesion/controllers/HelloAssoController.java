package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.helloasso.HelloAssoCheckoutRequest;
import com.wild.corp.adhesion.services.HelloAssoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/helloasso")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class HelloAssoController {

    private final HelloAssoService helloAssoService;

    @PostMapping("/checkout-intents")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createCheckoutIntent(Authentication principal, @Valid @RequestBody HelloAssoCheckoutRequest request) {
        log.info("createCheckoutIntent by {} for adhesionId={}", principal.getName(), request.getAdhesionId());
        return ResponseEntity.ok(helloAssoService.createCheckoutIntent(request));
    }

    @GetMapping("/checkout-intents/{checkoutIntentId}")
    @PreAuthorize("hasRole('USER') or hasRole('SECRETAIRE') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCheckoutIntent(Authentication principal, @PathVariable Integer checkoutIntentId) {
        log.info("getCheckoutIntent by {} for checkoutIntentId={}", principal.getName(), checkoutIntentId);
        return ResponseEntity.ok(helloAssoService.getCheckoutIntent(checkoutIntentId));
    }
}
