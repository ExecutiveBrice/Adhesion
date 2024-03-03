package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.AdherentLite;
import com.wild.corp.adhesion.models.Document;
import com.wild.corp.adhesion.services.AdherentServices;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/adherent")
public class AdherentController {

@Autowired
AdherentServices adherentServices;

	@GetMapping("/refreshAccords")
	public ResponseEntity<?> refreshAccords() {
		adherentServices.refreshAccords();
		return ResponseEntity.ok("refreshAccords");
	}

	@DeleteMapping("/deleteAdherent")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> deleteAdherent(@PathParam("adherentId") Long adherentId) {
		adherentServices.deleteAdherent(adherentId);
		return ResponseEntity.ok(adherentId+" deleted");
	}


	@PostMapping("/newAdherentDansTribu")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> newAdherentDansTribu(Authentication principal,@PathParam("tribuUuid") String tribuUuid) {
		return ResponseEntity.ok(adherentServices.newAdherentDansTribu(principal.getName(), tribuUuid));
	}

	@PostMapping("/changeTribu")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> changeTribu(Authentication principal,@RequestBody Long referentId, @PathParam("adherentId") Long adherentId) {
		adherentServices.addModification(principal.getName(),adherentId);
		return ResponseEntity.ok(adherentServices.changeTribu(referentId, adherentId));
	}

	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> update(Authentication principal, @RequestBody AdherentLite adherent) {
		adherentServices.addModification(principal.getName(), adherent.getId());
		return ResponseEntity.ok(adherentServices.update(adherent));
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
