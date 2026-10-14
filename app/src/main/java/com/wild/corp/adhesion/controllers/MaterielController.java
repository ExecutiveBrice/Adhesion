package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.services.MaterielServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/materiel")
public class MaterielController {
    private final MaterielServices service;

    public MaterielController(MaterielServices service) { this.service = service; }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail erreur(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBRECA', 'LOGISTIQUE')")
    public List<MaterielResponse> getAll() { return service.getAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public MaterielResponse create(@RequestBody MaterielRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public MaterielResponse update(@PathVariable Long id, @RequestBody MaterielRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/{id}/emprunt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBRECA')")
    public MaterielResponse emprunter(@PathVariable Long id, Authentication authentication,
            @RequestBody(required = false) EmpruntMaterielRequest request) {
        return service.emprunter(id, authentication.getName(), request == null ? null : request.emprunteurId(),
                administrateur(authentication));
    }

    @PostMapping("/{id}/retour")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBRECA')")
    public MaterielResponse restituer(@PathVariable Long id, Authentication authentication) {
        return service.restituer(id, authentication.getName(), administrateur(authentication));
    }

    @GetMapping("/membres-ca")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MembreCAMaterielResponse> getMembresCA() { return service.getMembresCA(); }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBRECA', 'LOGISTIQUE')")
    public List<EmpruntMaterielResponse> getHistorique(@PathVariable Long id) { return service.getHistorique(id); }

    @PatchMapping("/{id}/hors-service")
    @PreAuthorize("hasRole('ADMIN')")
    public MaterielResponse updateHorsService(@PathVariable Long id, @RequestBody HorsServiceRequest request,
            Authentication authentication) {
        return service.updateHorsService(id, request, authentication.getName());
    }

    private boolean administrateur(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/zonages")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBRECA', 'LOGISTIQUE')")
    public List<ZonageConfiguration> getZonages() { return service.getZonages(); }

    @PostMapping("/zonages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public ZonageConfiguration createZonage(@RequestBody ZonageConfiguration request) {
        return service.createZonage(request);
    }

    @PutMapping("/zonages/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public ZonageConfiguration updateZonage(@PathVariable Long id, @RequestBody ZonageConfiguration request) {
        return service.updateZonage(id, request);
    }

    @DeleteMapping("/zonages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'LOGISTIQUE')")
    public void deleteZonage(@PathVariable Long id) { service.deleteZonage(id); }
}
