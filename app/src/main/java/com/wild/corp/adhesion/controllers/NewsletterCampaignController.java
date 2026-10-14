package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.models.newsletter.NewsletterCampaign;
import com.wild.corp.adhesion.services.NewsletterCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@RequestMapping("newsletter/campaigns")
public class NewsletterCampaignController {
    private final NewsletterCampaignService service;
    public record Confirmation(String confirmationVersion) { }

    // Return upstream failures directly, without a servlet error dispatch which can mask them.
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail error(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
    public List<NewsletterCampaign> drafts() { return service.drafts(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
    public NewsletterCampaign preview(@PathVariable long id) { return service.preview(id); }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
    public Historique status(@PathVariable long id) { return service.status(id); }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
    public ResponseEntity<Historique> send(@PathVariable long id, @RequestBody Confirmation confirmation) {
        Historique history = service.send(id, confirmation.confirmationVersion());
        return ResponseEntity.status("ACCEPTED".equals(history.getCampaignStatus()) ? 202 : 200).body(history);
    }
}
