package com.gestion.user.controllers;

import com.gestion.user.models.Param;
import com.gestion.user.services.ParamServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/param")
public class ParamController {

@Autowired
ParamServices paramServices;

	@GetMapping("/all")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(paramServices.getAll());
	}

	@PostMapping("/save")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> save(@RequestBody Param param) {
		return ResponseEntity.ok(paramServices.save(param));
	}
}
