package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.SeanceServices;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/seance")
public class SeanceController {

    @Autowired
    SeanceServices seanceServices;

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Séances de l'adhérent pour la période demandée",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SeanceResponse.class)))
            )
    })
    @GetMapping
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
    public ResponseEntity<List<SeanceResponse>> getSeancesForAdherent(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam Long adherentId) {
        return ResponseEntity.ok(seanceServices.getSeancesForAdherent(dateDebut, dateFin, adherentId));
    }
}
