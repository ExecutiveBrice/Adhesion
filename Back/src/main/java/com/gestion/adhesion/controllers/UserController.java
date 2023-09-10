package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.ERole;
import com.gestion.adhesion.models.User;
import com.gestion.adhesion.repository.AdherentRepository;
import com.gestion.adhesion.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/user")
public class UserController {

@Autowired
	UserServices userServices;

	@Autowired
	AdherentRepository adherentRepository;


	@GetMapping("/addUserForAll")
	public ResponseEntity<?> addUserForAll() {
		userServices.addUserForAll();
		return ResponseEntity.ok("in progress");
	}

	@GetMapping("/connecteduser")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getConnectedUser(Authentication Authentication) {
		User user = userServices.findByEmail(Authentication.getName());
		return ResponseEntity.ok(user);
	}

	@GetMapping("/getUserByMail")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> getUserByMail(@PathParam("userId") String userEmail) {
		User user = userServices.findByEmail(userEmail);
		return ResponseEntity.ok(user);
	}

	@PostMapping("/grantUser")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> grantUser(@RequestBody String role, @PathParam("userEmail") String userEmail) {

		return ResponseEntity.ok(userServices.grantUser(ERole.valueOf(role), userServices.findByEmail(userEmail)));
	}

	@PostMapping("/unGrantUser")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> unGrantUser(@RequestBody String role, @PathParam("userEmail") String userEmail) {

		return ResponseEntity.ok(userServices.unGrantUser(ERole.valueOf(role), userServices.findByEmail(userEmail)));
	}

	@GetMapping("/allLite")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllLite() {
		return ResponseEntity.ok(userServices.getAllLite());
	}


}
