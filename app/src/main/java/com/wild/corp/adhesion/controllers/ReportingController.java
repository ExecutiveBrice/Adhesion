package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/reporting")
public class ReportingController {

	@Autowired
	ReportingService reportingService;

	@GetMapping("/getAllBasket")
	@PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'ADMINISTRATEUR', 'ADMIN', 'COMPTABLE')")
	public ResponseEntity<?> getAllBasket() {
		return ResponseEntity.ok(reportingService.getAllActiviteBasket());
	}

	@GetMapping("/getAllGeneral")
	@PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'ADMINISTRATEUR', 'ADMIN', 'COMPTABLE')")
	public ResponseEntity<?> getAllGeneral() {
		return ResponseEntity.ok(reportingService.getAllActiviteGeneral());
	}

	@GetMapping("/getAllAdhesions")
	@PreAuthorize("hasAnyRole('SECRETAIRE', 'BUREAU', 'ADMINISTRATEUR', 'ADMIN', 'COMPTABLE')")
	public ResponseEntity<?> getAllAdhesions() {
		LocalDate debut = LocalDate.of(2026,05,01);

		return ResponseEntity.ok(reportingService.getAllAdhesions(debut, LocalDate.now()));
	}

}
