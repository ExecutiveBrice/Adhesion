package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.Adherent;
import com.gestion.adhesion.models.Document;
import com.gestion.adhesion.models.Notification;
import com.gestion.adhesion.models.Role;
import com.gestion.adhesion.services.AdherentServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.websocket.server.PathParam;
import java.io.IOException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/adherent")
public class AdherentController {

@Autowired
AdherentServices adherentServices;

	@DeleteMapping("/deleteAdherent")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE') ")
	public ResponseEntity<?> deleteAdherent(@PathParam("adherentId") Long adherentId) {
		adherentServices.deleteAdherent(adherentId);
		return ResponseEntity.ok(adherentId+" deleted");
	}

	@DeleteMapping("/deleteDoc")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> deleteDoc(Authentication principal,@PathParam("docId") Long docId, @PathParam("adherentId") Long adherentId) {
		adherentServices.addModification(principal.getName(),adherentId);
		adherentServices.deleteDoc(docId, adherentId);
		return ResponseEntity.ok(docId+" deleted");
	}

	@PostMapping("/changeTribu")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> changeTribu(Authentication principal,@RequestBody Long referentId, @PathParam("adherentId") Long adherentId) {
		adherentServices.addModification(principal.getName(),adherentId);
		return ResponseEntity.ok(adherentServices.changeTribu(referentId, adherentId));
	}

	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> update(@RequestBody Adherent adherent, Authentication principal) {
		adherentServices.addModification(principal.getName(), adherent.getId());
		return ResponseEntity.ok(adherentServices.update(adherent));
	}

	@PostMapping("/addDocument")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addDocument(Authentication principal,@RequestBody MultipartFile file, @PathParam("adherentId") Long adherentId) throws IOException {
		Document response = adherentServices.addDocument(file, adherentId);
		adherentServices.addModification(principal.getName(), adherentId);
		return ResponseEntity.status(HttpStatus.OK)
				.body(response);
	}

	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(Authentication principal,@RequestBody Adherent adherent, @PathParam("tribuId") Long tribuId) {
		Adherent bddAdherent = adherentServices.save(adherent, tribuId);
		adherentServices.addModification(principal.getName(), bddAdherent.getId());
		return ResponseEntity.ok(bddAdherent);
	}

	@PostMapping("/addVisite")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addVisite(Authentication principal, @PathParam("adherentId") Long adherentId) {
		return ResponseEntity.ok(adherentServices.addVisite(principal.getName(), adherentId));
	}

	@GetMapping("/all")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(adherentServices.getAll());
	}

	@GetMapping("/allLite")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllLite() {
		return ResponseEntity.ok(adherentServices.getAllLite());
	}

	@GetMapping("/allId")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllId() {
		return ResponseEntity.ok(adherentServices.getAllId());
	}

	@GetMapping("/getAllCours")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getAllCours(Authentication principal) {
		return ResponseEntity.ok(adherentServices.getAllCours(principal.getName()));
	}

	@GetMapping("/getById")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getById(@PathParam("adherentId") Long adherentId) {
		return ResponseEntity.ok(adherentServices.getById(adherentId));
	}

	@GetMapping("/getByRole")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getById(@PathParam("roleId") Integer roleId) {
		return ResponseEntity.ok(adherentServices.getByRole(roleId));
	}

	@GetMapping("/addAccord")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> addAccord(Authentication principal, @PathParam("adherentId") Long adherentId, @PathParam("nomAccord") String nomAccord) {
		adherentServices.addModification(principal.getName(),adherentId);
		return ResponseEntity.ok(adherentServices.addAccord(adherentId, nomAccord));
	}

	@GetMapping("/removeAccord")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> removeAccord(Authentication principal, @PathParam("adherentId") Long adherentId, @PathParam("nomAccord") String nomAccord) {
		adherentServices.addModification(principal.getName(),adherentId);
		return ResponseEntity.ok(adherentServices.removeAccord(adherentId, nomAccord));
	}
}
