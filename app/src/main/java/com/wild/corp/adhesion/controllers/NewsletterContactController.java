package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.NewsletterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@RequestMapping("newsletter/contacts/sync")
public class NewsletterContactController {
    private final NewsletterSyncService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsletterSyncService.Progress> synchronize(Authentication principal) {
        log.warn("AUDIT action=newsletterContactSync actor={}", principal.getName());
        var progress = service.start();
        return ResponseEntity.status(progress.running() ? 202 : 200).body(progress);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public NewsletterSyncService.Progress status() { return service.status(); }
}
