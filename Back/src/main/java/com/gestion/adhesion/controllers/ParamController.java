package com.gestion.adhesion.controllers;

import com.gestion.adhesion.models.ParamBoolean;
import com.gestion.adhesion.models.ParamNumber;
import com.gestion.adhesion.models.ParamText;
import com.gestion.adhesion.services.ParamBooleanServices;
import com.gestion.adhesion.services.ParamNumberServices;
import com.gestion.adhesion.services.ParamTextServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api_adhesion/param")
public class ParamController {

	@Autowired
	ParamBooleanServices paramBooleanServices;

	@Autowired
	ParamTextServices paramTextServices;

	@Autowired
	ParamNumberServices paramNumberServices;

	@GetMapping("/allText")
	public ResponseEntity<?> getAllText() {
		return ResponseEntity.ok(paramTextServices.getAll());
	}

	@PostMapping("/saveText")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> saveText(@RequestBody ParamText param) {
		return ResponseEntity.ok(paramTextServices.save(param));
	}

	@GetMapping("/allBoolean")
	public ResponseEntity<?> getAllBoolean() {
		return ResponseEntity.ok(paramBooleanServices.getAll());
	}

	@GetMapping("/isClose")
	public ResponseEntity<?> isClose() {
		return ResponseEntity.ok(paramBooleanServices.isClose());
	}

	@PostMapping("/saveBoolean")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> saveBoolean(@RequestBody ParamBoolean param) {
		return ResponseEntity.ok(paramBooleanServices.save(param));
	}

	@GetMapping("/allNumber")
	public ResponseEntity<?> getAllNumber() {
		return ResponseEntity.ok(paramNumberServices.getAll());
	}

	@PostMapping("/saveNumber")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> saveNumber(@RequestBody ParamNumber param) {
		return ResponseEntity.ok(paramNumberServices.save(param));
	}
}
