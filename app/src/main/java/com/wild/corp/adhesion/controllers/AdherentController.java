package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.AdherentLite;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.UserServices;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

	import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/adherent")
@Slf4j
public class AdherentController {

@Autowired
AdherentServices adherentServices;
	@Autowired
	UserServices userServices;

//	@GetMapping("/nouvelleAnnee")
//	public ResponseEntity<?> nouvelleAnnee() {
//		adherentServices.nouvelleAnnee();
//		return ResponseEntity.ok("nouvelleAnnee");
//	}
//
//	@GetMapping("/refreshAccords")
//	public ResponseEntity<?> refreshAccords() {
//		adherentServices.refreshAccords();
//		return ResponseEntity.ok("refreshAccords");
//	}
//
//	@GetMapping("/cleanNotification")
//	public ResponseEntity<?> cleanNotification() {
//		adherentServices.cleanNotification();
//		return ResponseEntity.ok("refreshAccords");
//	}

	@DeleteMapping("/deleteAdherent")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> deleteAdherent(Authentication principal, @PathParam("adherentId") Long adherentId) {
		log.info("deleteAdherent by " + principal.getName() + " for adherent id "+adherentId);
		adherentServices.deleteAdherent(adherentId);
		return ResponseEntity.ok(adherentId+" deleted");
	}

	@PostMapping("/newAdherentDansTribu")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> newAdherentDansTribu(Authentication principal,@PathParam("tribuUuid") String tribuUuid) {
		log.info("newAdherentDansTribu by " + principal.getName() + " for tribuUuid id "+tribuUuid);
		return ResponseEntity.ok(adherentServices.newAdherentDansTribu(principal.getName(), tribuUuid));
	}

	@PostMapping("/update")
	@PreAuthorize("hasRole('USER')")
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> update(Authentication principal, @RequestBody AdherentLite adherent) {
		log.info("update by " + principal.getName() + " for adherent id "+adherent.getId());
		adherentServices.addModification(principal.getName(), adherent.getId(), "Mise à jour des données personnelles de l'adhérent");

		AdherentLite adh = null;
		try{
			adh = adherentServices.update(adherent);
		}catch (Exception p){
		if(p.getMessage().contains("duplicate key value violates unique constraint")){

			return ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate key value violates unique constraint");
		}
		}


		return ResponseEntity.ok(adh);
	}

	@PostMapping("/addVisite")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> addVisite(Authentication principal, @PathParam("adherentId") Long adherentId) {
		log.info("addVisite by " + principal.getName() + " for adherent id "+adherentId);
		return ResponseEntity.ok(adherentServices.addVisite(principal.getName(), adherentId));
	}

	@GetMapping("/all")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAll(Authentication principal) {
		log.info("getAll by " + principal.getName() );
		return ResponseEntity.ok(adherentServices.getAll());
	}

	@GetMapping("/regenerate")
	public ResponseEntity<?> regenerate( @PathParam("adherentId") Long adherentId) {
		log.info("regenerate by " );
		adherentServices.regenerate(adherentId);
		return ResponseEntity.ok("regenerate");
	}

	@GetMapping("/allLite")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllLite(Authentication principal) {
		log.info("getAllLite by " + principal.getName() );
		return ResponseEntity.ok(adherentServices.getAllLite());
	}

	@GetMapping("/allId")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getAllId(Authentication principal) {
		log.info("getAllId by " + principal.getName() );
		return ResponseEntity.ok(adherentServices.getAllId());
	}

	@GetMapping("/getAllCours")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getAllCours(Authentication principal) {
		log.info("getAllCours by " + principal.getName() );
		return ResponseEntity.ok(adherentServices.getAllCours(principal.getName()));
	}

	@GetMapping("/getById")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getById(Authentication principal, @PathParam("adherentId") Long adherentId) {
		log.info("getById by " + principal.getName() + " for adherent id "+adherentId);
		return ResponseEntity.ok(adherentServices.getById(adherentId));
	}

	@GetMapping("/getByRole")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
	public ResponseEntity<?> getByRole(Authentication principal, @PathParam("roleId") Integer roleId) {
		log.info("getByRole by " + principal.getName() + " for roleId id "+roleId);
		return ResponseEntity.ok(adherentServices.getByRole(roleId));
	}

	@GetMapping("/addAccord")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> addAccord(Authentication principal, @PathParam("adherentId") Long adherentId, @PathParam("nomAccord") String nomAccord) {
		log.info("addAccord by " + principal.getName() + " "+nomAccord+" for adherent id "+adherentId);
		adherentServices.addModification(principal.getName(),adherentId, "Ajout de l'accord "+nomAccord+" pour l'adhérent");
		return ResponseEntity.ok(adherentServices.addAccord(adherentId, nomAccord));
	}

	@GetMapping("/removeAccord")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> removeAccord(Authentication principal, @PathParam("adherentId") Long adherentId, @PathParam("nomAccord") String nomAccord) {
		log.info("removeAccord by " + principal.getName() + " "+nomAccord+" for adherent id "+adherentId);
		adherentServices.addModification(principal.getName(),adherentId, "Suppression de l'accord "+nomAccord+" pour l'adhérent");
		return ResponseEntity.ok(adherentServices.removeAccord(adherentId, nomAccord));
	}

}
