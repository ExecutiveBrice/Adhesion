package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.resources.AjoutSeancesRequest;
import com.wild.corp.adhesion.models.resources.MiseAJourSeanceRequest;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.GoogleAgendaServices;
import com.wild.corp.adhesion.services.SeanceServices;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/activite")
@Slf4j
public class ActiviteController {

@Autowired
ActiviteServices activiteServices;
	@Autowired
	SeanceServices seanceServices;
	@Autowired
	GoogleAgendaServices googleAgendaServices;
	@GetMapping("/all")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(activiteServices.getAll());
	}

	@GetMapping("/allNm1")
	public ResponseEntity<?> getAllNm1() {
		return ResponseEntity.ok(activiteServices.getAllNm1());
	}

	@GetMapping("/seancesDuJour")
	public ResponseEntity<?> getSeancesDuJourForAcivite( @RequestParam(value="activiteId") Long activiteId) {
		log.info("getAllCours for activite " + activiteId );
		return ResponseEntity.ok(activiteServices.getSeancesDuJour(activiteId));
	}

	@GetMapping("/calendrier")
	public ResponseEntity<?> getCalendrier(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam(required = false) java.util.UUID tribuUuid) {
		return ResponseEntity.ok(seanceServices.getCalendrier(dateDebut, dateFin, tribuUuid));
	}

	@GetMapping("/calendrier/google")
	public ResponseEntity<?> getCalendrierGoogle(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam("source") List<String> sources) {
		return ResponseEntity.ok(googleAgendaServices.getCalendrier(dateDebut, dateFin, sources));
	}

	@GetMapping("/{activiteId}/seances")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getSeances(@PathVariable Long activiteId) {
		return ResponseEntity.ok(activiteServices.getSeances(activiteId));
	}

	@PostMapping("/{activiteId}/seances")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addSeances(@PathVariable Long activiteId,
			@RequestBody @Valid AjoutSeancesRequest request) {
		return ResponseEntity.ok(activiteServices.addSeances(
				activiteId, request.nombreSeances(), request.dateDebut()));
	}

	@PatchMapping("/{activiteId}/seances/{seanceId}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> updateSeance(@PathVariable Long activiteId,
			@PathVariable Long seanceId,
			@RequestBody @Valid MiseAJourSeanceRequest request) {
		return ResponseEntity.ok(SeanceResponse.from(seanceServices.updateSeance(
				activiteId, seanceId, request.etatSeance(), request.commentaire(), request.commentairePresent(),
				request.date(), request.heureDebut(), request.horairePresent())));
	}

	@DeleteMapping("/{activiteId}/seances/{seanceId}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<Void> deleteSeance(@PathVariable Long activiteId, @PathVariable Long seanceId) {
		seanceServices.deleteSeance(activiteId, seanceId);
		return ResponseEntity.noContent().build();
	}


	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(@RequestBody Activite activite) {
		return ResponseEntity.ok(activiteServices.save(activite));
	}

	@GetMapping("/refillSeance")
	public ResponseEntity<?> refillSeance(@RequestParam(value="activiteId") Long activiteId) {
		Activite activiteInDB = activiteServices.getById(activiteId);
		seanceServices.fillSeances(activiteInDB, 29);
		return ResponseEntity.ok(activiteInDB);
	}


	@PostMapping("/addReferent")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save( @RequestParam(value="activiteId") Long activiteId, @RequestParam(value="adherentId") Long adherentId ) {
		return ResponseEntity.ok(activiteServices.addReferent(activiteId, adherentId));
	}


}
