package com.gestion.adhesion.controllers;

import com.gestion.adhesion.services.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/reporting")
public class ReportingController {

	@Autowired
	ReportingService reportingService;

	@GetMapping("/getAllBasket")
	public ResponseEntity<?> getAllBasket() {
		return ResponseEntity.ok(reportingService.getAllActiviteBasket());
	}

	@GetMapping("/getAllGeneral")
	public ResponseEntity<?> getAllGeneral() {
		return ResponseEntity.ok(reportingService.getAllActiviteGeneral());
	}

	@GetMapping("/getAllAdhesions")
	public ResponseEntity<?> getAllAdhesions() {
		LocalDate debut = LocalDate.of(2023,05,01);

		return ResponseEntity.ok(reportingService.getAllAdhesions(debut, LocalDate.now()));
	}

}
