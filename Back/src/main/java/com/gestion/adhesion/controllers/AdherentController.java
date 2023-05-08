package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.Adherent;
import com.gestion.adhesion.models.Document;
import com.gestion.adhesion.services.AdherentServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteAdherent(@PathParam("adherentId") Long adherentId) {
		adherentServices.deleteAdherent(adherentId);
		return ResponseEntity.ok(adherentId+" deleted");
	}
	@PostMapping("/changeTribu")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> changeTribu(@RequestBody Long referentId, @PathParam("adherentId") Long adherentId) {

		return ResponseEntity.ok(adherentServices.changeTribu(referentId, adherentId));
	}
	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> update(@RequestBody Adherent adherent) {
		return ResponseEntity.ok(adherentServices.update(adherent));
	}



	@PostMapping("/addDocument")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addDocument(@RequestBody MultipartFile file, @PathParam("adherentId") Long adherentId) throws IOException {
		Document response = adherentServices.addDocument(file, adherentId);

		return ResponseEntity.status(HttpStatus.OK)
				.body(response);
	}
	@PostMapping("/save")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> save(@RequestBody Adherent adherent, @PathParam("tribuId") Long tribuId) {
		return ResponseEntity.ok(adherentServices.save(adherent, tribuId));
	}

	@PostMapping("/updateEmail")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> updateEmail(@RequestBody String email, @PathParam("adherentId") Long adherentId) {
		return ResponseEntity.ok(adherentServices.updateEmail(email, adherentId));
	}
	@GetMapping("/all")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(adherentServices.getAll());
	}

	@GetMapping("/getById")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getById(@PathParam("adherentId") Long adherentId) {
		return ResponseEntity.ok(adherentServices.getById(adherentId));
	}

}
