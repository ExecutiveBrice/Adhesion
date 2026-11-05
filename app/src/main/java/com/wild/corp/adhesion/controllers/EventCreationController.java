package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.EventCreationService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@RestController @RequestMapping("/teams") @PreAuthorize("isAuthenticated()")
public class EventCreationController {
    private final EventCreationService service;
    public EventCreationController(EventCreationService service) { this.service = service; }
    @GetMapping("/event-options") public EventCreationService.Options options(Principal principal) { return service.options(principal.getName()); }
    @PostMapping("/events") public EventCreationService.Created create(@RequestBody EventCreationService.Creation request, Principal principal) {
        return service.create(request, principal.getName());
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason()));
    }
}
