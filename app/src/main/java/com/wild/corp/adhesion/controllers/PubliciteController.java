package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.repository.PubliciteRepository.Publicite;
import com.wild.corp.adhesion.services.PubliciteService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/publicites")
public class PubliciteController {
    private final PubliciteService service;

    public PubliciteController(PubliciteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Publicite> list(Authentication authentication) {
        return service.list(authentication.getName(), canManage(authentication));
    }

    @GetMapping("/{id}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> image(@PathVariable long id, Authentication authentication) {
        var image = service.image(id, authentication.getName(), canManage(authentication));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore()).body(image.bytes());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMMUNICATION_GLOBAL')")
    public Publicite create(@RequestParam String description,
                            @RequestParam(required = false) String facebookUrl,
                            @RequestParam(required = false) String snapchatUrl,
                            @RequestParam(required = false) String websiteUrl,
                            @RequestParam(required = false) List<Long> sectionIds,
                            @RequestParam(defaultValue = "0") Integer displayOrder,
                            @RequestParam(defaultValue = "true") boolean active,
                            @RequestParam MultipartFile file) {
        return service.create(description, facebookUrl, snapchatUrl, websiteUrl, sectionIds, displayOrder, active, file);
    }

    public record Settings(Integer displayOrder, Boolean active) {}

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COMMUNICATION_GLOBAL')")
    public Publicite update(@PathVariable long id,
                            @RequestParam String description,
                            @RequestParam(required = false) String facebookUrl,
                            @RequestParam(required = false) String snapchatUrl,
                            @RequestParam(required = false) String websiteUrl,
                            @RequestParam(required = false) List<Long> sectionIds,
                            @RequestParam Integer displayOrder,
                            @RequestParam boolean active,
                            @RequestParam(required = false) MultipartFile file) {
        return service.update(id, description, facebookUrl, snapchatUrl, websiteUrl, sectionIds, displayOrder,
                active, file);
    }

    @PutMapping("/{id}/settings")
    @PreAuthorize("hasRole('COMMUNICATION_GLOBAL')")
    public Publicite updateSettings(@PathVariable long id, @RequestBody Settings settings) {
        return service.updateSettings(id, settings.displayOrder(), settings.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COMMUNICATION_GLOBAL')")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    private static boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_COMMUNICATION_GLOBAL".equals(authority.getAuthority()));
    }
}
