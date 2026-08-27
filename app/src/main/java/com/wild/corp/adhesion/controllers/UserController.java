package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.UserServices;
import com.wild.corp.adhesion.services.PresenceServices;
import com.wild.corp.adhesion.services.SeanceServices;
import com.wild.corp.adhesion.models.resources.AjoutAdherentSeanceRequest;
import com.wild.corp.adhesion.models.resources.CommentaireSeanceRequest;
import com.wild.corp.adhesion.models.resources.PresenceSeanceResponse;
import com.wild.corp.adhesion.models.resources.SeanceDuJourResponse;
import com.wild.corp.adhesion.models.resources.PresenceUpdateRequest;
import com.wild.corp.adhesion.utils.Status;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

@Autowired
UserServices userServices;
@Autowired
PresenceServices presenceServices;
@Autowired
SeanceServices seanceServices;

@Autowired
AdhesionServices adhesionServices;

@Autowired
SeanceRepository seanceRepository;

	@Autowired
	AdherentRepository adherentRepository;


	@GetMapping("/seancesDuJour")
	@PreAuthorize("hasAnyRole('PROF', 'REFERENT')")
	public ResponseEntity<?> getSeancesDuJour(Authentication principal) {
		log.info("getAllCours by " + principal.getName() );
		return ResponseEntity.ok(userServices.getSeancesDuJourForUser(principal.getName()));
	}

	@GetMapping("/secretariat/seances")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> getSeancesDuJourPourLeSecretariat(
			@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
		return ResponseEntity.ok(userServices.getSeancesDuJourForSecretary(date));
	}

	@GetMapping("/secretariat/seances/{seanceId}/presences")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> getPresencesPourLeSecretariat(@PathVariable Long seanceId) {
		return ResponseEntity.ok(presenceServices.getPresencesForSecretary(seanceId));
	}

	@PatchMapping("/secretariat/seances/{seanceId}/presences/{presenceId}")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> updatePresencePourLeSecretariat(@PathVariable Long seanceId, @PathVariable Long presenceId,
			@RequestBody PresenceUpdateRequest request) {
		return ResponseEntity.ok(presenceServices.updatePresenceForSecretary(seanceId, presenceId, request.presence()));
	}

	@PatchMapping("/secretariat/seances/{seanceId}/commentaire")
	@PreAuthorize("hasRole('SECRETAIRE')")
	public ResponseEntity<?> updateCommentairePourLeSecretariat(@PathVariable Long seanceId,
			@RequestBody CommentaireSeanceRequest request) {
		return ResponseEntity.ok(SeanceDuJourResponse.from(
				seanceServices.updateCommentaireForSecretary(seanceId, request.commentaire())));
	}

	@GetMapping("/seances/{seanceId}/presences")
	@PreAuthorize("hasAnyRole('PROF', 'REFERENT')")
	public ResponseEntity<?> getPresences(@PathVariable Long seanceId, Authentication principal) {
		return ResponseEntity.ok(presenceServices.getPresences(seanceId, principal.getName()));
	}

	@PatchMapping("/seances/{seanceId}/presences/{presenceId}")
	@PreAuthorize("hasAnyRole('PROF', 'REFERENT')")
	public ResponseEntity<?> updatePresence(@PathVariable Long seanceId, @PathVariable Long presenceId,
			@RequestBody PresenceUpdateRequest request, Authentication principal) {
		return ResponseEntity.ok(presenceServices.updatePresence(seanceId, presenceId, request.presence(), principal.getName()));
	}

	@PatchMapping("/seances/{seanceId}/commentaire")
	@PreAuthorize("hasAnyRole('PROF', 'REFERENT')")
	public ResponseEntity<?> updateCommentaire(@PathVariable Long seanceId,
			@RequestBody CommentaireSeanceRequest request, Authentication principal) {
		return ResponseEntity.ok(SeanceDuJourResponse.from(
				seanceServices.updateCommentaireForManager(seanceId, request.commentaire(), principal.getName())));
	}

	@PostMapping("/seances/{seanceId}/adherents")
	@PreAuthorize("hasAnyRole('PROF', 'REFERENT')")
	@Transactional
	public ResponseEntity<PresenceSeanceResponse> ajouterNouvelAdherent(
			@PathVariable Long seanceId,
			@Valid @RequestBody AjoutAdherentSeanceRequest request,
			Authentication principal) {
		String email = request.email().trim().toLowerCase();
		Seance seance = seanceRepository.findByIdAndManagerUsername(seanceId, principal.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable"));
		if (userServices.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée");
		}

		Adherent adherent = userServices.createUserAnonymous(email);
		Adhesion adhesion = adhesionServices.save(principal::getName, adherent.getId(), seance.getActivite().getId());
		// Un ajout depuis une séance doit toujours rester à compléter par l'adhérent,
		// y compris lorsque l'activité est complète.
		adhesion = adhesionServices.choisirStatut(adhesion.getId(), Status.ATTENTE_ADHERENT.label);
		Presence presence = presenceServices.addPresenceForSeance(adhesion, seance, true);
		adhesionServices.addModification(principal.getName(), adhesion.getId(), "Ajout à la séance " + seanceId);
		return ResponseEntity.status(HttpStatus.CREATED).body(PresenceSeanceResponse.from(presence));
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
