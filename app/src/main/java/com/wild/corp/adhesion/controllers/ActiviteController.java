package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.services.ActiviteServices;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/activite")
public class ActiviteController {

@Autowired
ActiviteServices activiteServices;

	@GetMapping("/all")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(activiteServices.getAll());
	}

	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(@RequestBody Activite activite) {
		return ResponseEntity.ok(activiteServices.save(activite));
	}

	@PostMapping("/addReferent")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save( @RequestParam(value="activiteId") Long activiteId, @RequestParam(value="adherentId") Long adherentId ) {
		return ResponseEntity.ok(activiteServices.addReferent(activiteId, adherentId));
	}


}
