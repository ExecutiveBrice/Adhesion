package com.wild.corp.adhesion.controllers;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.SeanceServices;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/evenements")
public class EvenementController {
    private final SeanceServices events;
    public EvenementController(SeanceServices events) { this.events = events; }
    @GetMapping @PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('MEMBRECA') or hasRole('ADMIN')")
    public List<SeanceResponse> forMember(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin, @RequestParam Long adherentId) {
        return events.getSeancesForAdherent(dateDebut, dateFin, adherentId);
    }
}
