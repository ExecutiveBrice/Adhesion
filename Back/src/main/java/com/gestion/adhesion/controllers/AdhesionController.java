package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.Adhesion;
import com.gestion.adhesion.models.Paiement;
import com.gestion.adhesion.services.AdhesionServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/adhesion")
public class AdhesionController {

@Autowired
AdhesionServices adhesionServices;


	@GetMapping("/updateTypePaiement")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> updateTypePaiement(@PathParam("inscriptionId") Long adhesionId, @PathParam("typePaiement") String typePaiement) {
		return ResponseEntity.ok(adhesionServices.updateTypePaiement(adhesionId, typePaiement));
	}


	@GetMapping("/addAccord")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> addAccord(@PathParam("inscriptionId") Long adhesionId, @PathParam("nomAccord") String nomAccord) {
		return ResponseEntity.ok(adhesionServices.addAccord(adhesionId, nomAccord));
	}

	@GetMapping("/removeAccord")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> removeAccord(@PathParam("inscriptionId") Long adhesionId, @PathParam("nomAccord") String nomAccord) {
		return ResponseEntity.ok(adhesionServices.removeAccord(adhesionId, nomAccord));
	}

	@GetMapping("/changeActivite")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> changeActivite(@PathParam("inscriptionId") Long adhesionId, @PathParam("activiteId") Long activiteId) {
		return ResponseEntity.ok(adhesionServices.changeActivite(adhesionId, activiteId));
	}



	@DeleteMapping("/deleteAdhesion")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteAdhesion(@PathParam("adhesionId") Long adhesionId) {
		adhesionServices.deleteAdhesion(adhesionId);
		return ResponseEntity.ok(adhesionId+" deleted");
	}

	@GetMapping("/updateDocumentsSecretariat")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updateDocumentsSecretariat(@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {
		return ResponseEntity.ok(adhesionServices.updateDocumentsSecretariat(adhesionId, statut));
	}

	@GetMapping("/updatePaiementSecretariat")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updatePaiementSecretariat(@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {

		return ResponseEntity.ok(adhesionServices.updatePaiementSecretariat(adhesionId, statut));
	}

	@GetMapping("/updateFlag")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updateFlag(@PathParam("inscriptionId") Long adhesionId, @PathParam("statut") Boolean statut) {
		return ResponseEntity.ok(adhesionServices.updateFlag(adhesionId, statut));
	}


	@GetMapping("/enregistrerRemarque")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> enregistrerRemarque(@PathParam("inscriptionId") Long adhesionId, @PathParam("remarqueSecretariat") String remarqueSecretariat) {
		return ResponseEntity.ok(adhesionServices.enregistrerRemarque(adhesionId, remarqueSecretariat));
	}

	@GetMapping("/choisirStatut")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> choisirStatut(@PathParam("inscriptionId") Long adhesionId, @PathParam("statutActuel") String statutActuel) {
		return ResponseEntity.ok(adhesionServices.choisirStatut(adhesionId, statutActuel));
	}



	@GetMapping("/transfertPaiement")
	public ResponseEntity<?> transfertPaiement() {
		adhesionServices.transfertPaiement();
		return ResponseEntity.ok("ok");
	}



	@GetMapping("/all")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllLite() {
		return ResponseEntity.ok(adhesionServices.getAllLite());
	}


	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(@RequestBody List<Adhesion> adhesions) {
		return ResponseEntity.ok(adhesionServices.save(adhesions));
	}

	@PostMapping("/savePaiement")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> savePaiement(@PathParam("adhesionId") Long adhesionId, @RequestBody Paiement paiement) {
		return ResponseEntity.ok(adhesionServices.savePaiement(adhesionId, paiement));
	}

	@DeleteMapping("/deletePaiement")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> deletePaiement(@PathParam("adhesionId") Long adhesionId, @PathParam("paiementId") Long paiementId) {
		adhesionServices.deletePaiement(adhesionId, paiementId);
		return ResponseEntity.ok(paiementId+" deleted");
	}

	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> update(@RequestBody Adhesion adhesion) {
		return ResponseEntity.ok(adhesionServices.update(adhesion));
	}
}
