package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ParamBoolean;
import com.wild.corp.adhesion.models.ParamNumber;
import com.wild.corp.adhesion.models.ParamText;
import com.wild.corp.adhesion.services.ParamBooleanServices;
import com.wild.corp.adhesion.services.ParamNumberServices;
import com.wild.corp.adhesion.services.ParamTextServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/param")
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

	@GetMapping("/notification")
	public ResponseEntity<?> notification(@RequestBody Object param) {
		log.error(param.toString());
		return ResponseEntity.ok(param.toString());
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
