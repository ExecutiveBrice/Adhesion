package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.ComptaService;
import com.wild.corp.adhesion.models.resources.ComptaPeriode.PaiementCompta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/compta")
public class ComptaController {

	@Autowired
	ComptaService comptaService;

	@GetMapping("/getAll")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
	public ResponseEntity<?> getAll(@RequestParam("dateDebutPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutPeriode, @RequestParam("dateFinPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFinPeriode) {
		if (dateDebutPeriode.isAfter(dateFinPeriode)) {
			return ResponseEntity.badRequest().body("La date de début doit être antérieure ou égale à la date de fin.");
		}
		return ResponseEntity.ok(comptaService.getAll(dateDebutPeriode, dateFinPeriode));
	}

	@GetMapping("/periode")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
	public ResponseEntity<?> getPeriode(@RequestParam("dateDebutPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutPeriode,
			@RequestParam("dateFinPeriode") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFinPeriode) {
		if (dateDebutPeriode.isAfter(dateFinPeriode)) {
			return ResponseEntity.badRequest().body("La date de début doit être antérieure ou égale à la date de fin.");
		}
		return ResponseEntity.ok(comptaService.getPeriode(dateDebutPeriode, dateFinPeriode));
	}

	@PutMapping("/adhesions/{adhesionId}/rapprochement")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
	public ResponseEntity<Void> updateRapprochement(@PathVariable Long adhesionId,
			@RequestParam("rapprochement") boolean rapprochement, Authentication utilisateur) {
		comptaService.updateRapprochement(adhesionId, rapprochement, utilisateur.getName());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/adhesions/{adhesionId}/paiements")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
	public ResponseEntity<Void> savePaiement(@PathVariable Long adhesionId, @RequestBody PaiementCompta paiement,
			Authentication utilisateur) {
		comptaService.savePaiement(adhesionId, paiement, utilisateur.getName());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/adhesions/{adhesionId}/paiements/{paiementId}")
	@PreAuthorize("hasAnyRole('COMPTABLE', 'BUREAU', 'MEMBRECA', 'ADMIN')")
	public ResponseEntity<Void> deletePaiement(@PathVariable Long adhesionId, @PathVariable Long paiementId,
			Authentication utilisateur) {
		comptaService.deletePaiement(adhesionId, paiementId, utilisateur.getName());
		return ResponseEntity.noContent().build();
	}

}
