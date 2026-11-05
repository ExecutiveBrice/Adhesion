package com.wild.corp.adhesion.controllers;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.services.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/activite")
public class ActiviteController {
    private final ActiviteServices activities;
    private final SeanceServices events;
    private final GoogleAgendaServices google;
    public ActiviteController(ActiviteServices activities, SeanceServices events, GoogleAgendaServices google) {
        this.activities = activities; this.events = events; this.google = google;
    }
    @GetMapping("/all") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> all() { return ResponseEntity.ok(activities.getAll()); }
    @GetMapping("/{activiteId}") @PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
    public ResponseEntity<?> detail(@PathVariable Long activiteId) {
        return ResponseEntity.ok(activities.getDetail(activiteId));
    }
    @GetMapping("/managed/{activiteId}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> managedDetail(Authentication user, @PathVariable Long activiteId) {
        return ResponseEntity.ok(activities.getManagedDetail(user.getName(), activiteId));
    }
    @GetMapping("/page") @PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
    public ResponseEntity<?> page(@RequestParam(required = false) String search, @RequestParam(required = false) Integer tarif,
            @RequestParam(required = false) Boolean complete, @RequestParam(required = false) Boolean reinscription,
            @RequestParam(required = false) Integer age, @RequestParam(required = false) String genre,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(activities.getPage(search, tarif, complete, reinscription, age, genre, pageable));
    }
    @GetMapping("/managed/page") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> managedPage(Authentication user, @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer tarif, @RequestParam(required = false) Boolean complete,
            @RequestParam(required = false) Boolean reinscription, @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String genre, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(activities.getManagedPage(user.getName(), search, tarif, complete, reinscription, age, genre, pageable));
    }
    @GetMapping("/managed/encadrants") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> encadrants(Authentication user) { return ResponseEntity.ok(activities.getManagedEncadrants(user.getName())); }
    @GetMapping("/allNm1") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> previousYear() { return ResponseEntity.ok(activities.getAllNm1()); }
    @GetMapping("/calendrier")
    public ResponseEntity<List<SeanceCalendrierResponse>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Long adherentId, @RequestParam(required = false) UUID tribuUuid) {
        return ResponseEntity.ok(adherentId == null ? events.getCalendrier(dateDebut, dateFin, tribuUuid)
                : events.getCalendrierForAdherent(dateDebut, dateFin, adherentId));
    }
    @GetMapping("/calendrier/google")
    public ResponseEntity<CalendrierGoogleResponse> googleCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin, @RequestParam("source") List<String> sources) {
        return ResponseEntity.ok(google.getCalendrier(dateDebut, dateFin, sources));
    }
    @GetMapping("/{activiteId}/referents/candidats") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> candidates(@PathVariable Long activiteId) { return ResponseEntity.ok(activities.getReferentsCandidates(activiteId)); }
    @PostMapping("/save") @PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
    public ResponseEntity<?> save(@RequestBody Activite activity) { return ResponseEntity.ok(activities.save(activity)); }
    @PostMapping("/managed/save") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveManaged(Authentication user, @RequestBody Activite activity) { return ResponseEntity.ok(activities.saveManaged(user.getName(), activity)); }
    @PostMapping("/addReferent") @PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
    public ResponseEntity<?> addReferent(@RequestParam Long activiteId, @RequestParam Long adherentId) { return ResponseEntity.ok(activities.addReferent(activiteId, adherentId)); }
}
