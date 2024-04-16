package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.AdhesionLite;
import com.wild.corp.adhesion.models.Paiement;
import com.wild.corp.adhesion.services.AdhesionServices;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/adhesion")
public class AdhesionController {

@Autowired
AdhesionServices adhesionServices;



	@GetMapping("/addAccord")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> addAccord(Authentication principal, @PathParam("inscriptionId") Long adhesionId, @PathParam("nomAccord") String nomAccord) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.addAccord(adhesionId, nomAccord));
	}

	@GetMapping("/removeAccord")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> removeAccord(Authentication principal, @PathParam("inscriptionId") Long adhesionId, @PathParam("nomAccord") String nomAccord) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.removeAccord(adhesionId, nomAccord));
	}

	@GetMapping("/changeActivite")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> changeActivite(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("activiteId") Long activiteId) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.changeActivite(adhesionId, activiteId));
	}

	@DeleteMapping("/deleteAdhesion")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> deleteAdhesion(@PathParam("adhesionId") Long adhesionId) {
		adhesionServices.deleteAdhesion(adhesionId);
		return ResponseEntity.ok(adhesionId+" deleted");
	}

	@GetMapping("/updateDocumentsSecretariat")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updateDocumentsSecretariat(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.updateDocumentsSecretariat(adhesionId, statut));
	}

	@GetMapping("/updatePaiementSecretariat")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updatePaiementSecretariat(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.updatePaiementSecretariat(adhesionId, statut));
	}

	@GetMapping("/updateFlag")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updateFlag(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.updateFlag(adhesionId, statut));
	}

	@PostMapping("/addVisite")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addVisite(Authentication principal, @PathParam("adhesionId") Long adhesionId) {
		return ResponseEntity.ok(adhesionServices.addVisite(principal.getName(), adhesionId));
	}

	@GetMapping("/enregistrerRemarque")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> enregistrerRemarque(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("remarqueSecretariat") String remarqueSecretariat) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.enregistrerRemarque(adhesionId, remarqueSecretariat));
	}

	@GetMapping("/choisirStatut")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> choisirStatut(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("statutActuel") String statutActuel) throws IOException {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.choisirStatut(adhesionId, statutActuel));
	}

	@GetMapping("/all")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllLite() {
		return ResponseEntity.ok(adhesionServices.getAllLite());
	}

	@GetMapping("/liteBysection")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getLiteBySection(@PathParam("sections") String sections) {
		return ResponseEntity.ok(adhesionServices.getLiteBySection(sections));
	}

	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(Authentication principal, @PathParam("adherentId") Long adherentId, @PathParam("activiteId") Long activiteId) {
		Adhesion adhesionsBDD = adhesionServices.save(adherentId, activiteId);
		adhesionServices.addModification(principal.getName(), adhesionsBDD.getId());
		return ResponseEntity.ok(adhesionsBDD);
	}

	@PostMapping("/savePaiement")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> savePaiement(Authentication principal, @PathParam("adhesionId") Long adhesionId, @RequestBody Paiement paiement) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.savePaiement(adhesionId, paiement));
	}

	@DeleteMapping("/deletePaiement")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> deletePaiement(Authentication principal, @PathParam("adhesionId") Long adhesionId, @PathParam("paiementId") Long paiementId) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		adhesionServices.deletePaiement(adhesionId, paiementId);
		return ResponseEntity.ok(paiementId+" deleted");
	}

	@PostMapping("/saveSurclassement")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> saveSurclassement(Authentication principal, @PathParam("adhesionId") Long adhesionId, @PathParam("surClassementId") Long surClassementId) throws IOException {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.saveSurclassement(adhesionId, surClassementId));
	}

	@DeleteMapping("/deleteSurclassement")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> deleteSurclassement(Authentication principal, @PathParam("adhesionId") Long adhesionId) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		adhesionServices.deleteSurclassement(adhesionId);
		return ResponseEntity.ok("Surclassement deleted");
	}
	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> update(Authentication principal, @RequestBody Adhesion adhesion) {
		adhesionServices.addModification(principal.getName(), adhesion.getId());
		return ResponseEntity.ok(adhesionServices.update(adhesion));
	}

	@PostMapping("/validation")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> validation(Authentication principal, @PathParam("adhesionId") Long adhesionId, @RequestBody List<Accord> accords) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.validation(accords, adhesionId));
	}
}