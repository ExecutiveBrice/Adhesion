package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.SeanceServices;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
	@GetMapping("/all")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(activiteServices.getAll());
	}

	@GetMapping("/seancesDuJour")
	public ResponseEntity<?> getSeancesDuJourForAcivite( @RequestParam(value="activiteId") Long activiteId) {
		log.info("getAllCours for activite " + activiteId );
		return ResponseEntity.ok(activiteServices.getSeancesDuJour(activiteId));
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
