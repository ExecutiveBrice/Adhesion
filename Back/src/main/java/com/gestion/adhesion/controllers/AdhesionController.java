package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.Adhesion;
import com.gestion.adhesion.models.Paiement;
import com.gestion.adhesion.services.AdhesionServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/adhesion")
public class AdhesionController {

@Autowired
AdhesionServices adhesionServices;

	@GetMapping("/updateTypePaiement")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('USER')")
	public ResponseEntity<?> updateTypePaiement(Authentication principal,
												@PathParam("inscriptionId") Long adhesionId, @PathParam("typePaiement") String typePaiement) {
		adhesionServices.addModification(principal.getName(), adhesionId);
		return ResponseEntity.ok(adhesionServices.updateTypePaiement(adhesionId, typePaiement));
	}

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
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
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
	public ResponseEntity<?> choisirStatut(Authentication principal,@PathParam("inscriptionId") Long adhesionId, @PathParam("statutActuel") String statutActuel) {
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
	public ResponseEntity<?> save(Authentication principal, @RequestBody List<Adhesion> adhesions) {
		List<Adhesion> adhesionsBDD = adhesionServices.save(adhesions);
		adhesionsBDD.forEach(adhesion -> adhesionServices.addModification(principal.getName(), adhesion.getId()));
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

	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> update(Authentication principal, @RequestBody Adhesion adhesion) {
		adhesionServices.addModification(principal.getName(), adhesion.getId());
		return ResponseEntity.ok(adhesionServices.update(adhesion));
	}
}