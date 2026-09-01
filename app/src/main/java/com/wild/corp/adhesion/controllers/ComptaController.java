package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ComptaService;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/compta")
public class ComptaController {

	@Autowired
	ComptaService comptaService;

	@GetMapping("/getAll")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'ADMINISTRATEUR', 'ADMIN')")
	public ResponseEntity<?> getAll(@PathParam("dateDebutPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate dateDebutPeriode, @PathParam("dateFinPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate dateFinPeriode) {


		return ResponseEntity.ok(comptaService.getAll(dateDebutPeriode, dateFinPeriode));
	}

}
