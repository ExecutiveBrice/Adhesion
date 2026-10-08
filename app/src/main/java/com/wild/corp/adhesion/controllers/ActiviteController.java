package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.resources.*;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.GoogleAgendaServices;
import com.wild.corp.adhesion.services.SeanceServices;
import com.wild.corp.adhesion.services.ResponsabiliteSeanceServices;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/activite")
@Slf4j
public class ActiviteController {

@Autowired
ActiviteServices activiteServices;
	@Autowired
	SeanceServices seanceServices;
	@Autowired
	GoogleAgendaServices googleAgendaServices;
	@Autowired
	ResponsabiliteSeanceServices responsabiliteSeanceServices;
	@GetMapping("/all")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getAll() {
		return ResponseEntity.ok(activiteServices.getAll());
	}

	@GetMapping("/page")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('BUREAU') or hasRole('MEMBRECA') or hasRole('ADMIN')")
	public ResponseEntity<?> getPage(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) Integer tarif,
			@RequestParam(required = false) Boolean complete,
			@RequestParam(required = false) Boolean reinscription,
			@RequestParam(required = false) Integer age,
			@RequestParam(defaultValue = "") String genre,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(activiteServices.getPage(search, tarif, complete, reinscription, age, genre, pageable));
	}

	@GetMapping("/managed/page")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> getManagedPage(Authentication principal,
			@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) Integer tarif,
			@RequestParam(required = false) Boolean complete,
			@RequestParam(required = false) Boolean reinscription,
			@RequestParam(required = false) Integer age,
			@RequestParam(defaultValue = "") String genre,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(activiteServices.getManagedPage(principal.getName(), search, tarif, complete,
				reinscription, age, genre, pageable));
	}

	@GetMapping("/managed/encadrants")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> getManagedEncadrants(Authentication principal) {
		return ResponseEntity.ok(activiteServices.getManagedEncadrants(principal.getName()));
	}

	@GetMapping("/allNm1")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getAllNm1() {
		return ResponseEntity.ok(activiteServices.getAllNm1());
	}

	@GetMapping("/seancesDuJour")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getSeancesDuJour( @RequestParam(value="activiteId") Long activiteId) {
		log.info("getAllCours for activite " + activiteId );
		return ResponseEntity.ok(activiteServices.getSeancesDuJour(activiteId));
	}

	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "successful operation",
					content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SeanceCalendrierResponse.class)))
			),
	})
	@GetMapping("/calendrier")
	public ResponseEntity<List<SeanceCalendrierResponse>> getCalendrier(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam(required = false) Long adherentId,
			@RequestParam(required = false) java.util.UUID tribuUuid) {
		return ResponseEntity.ok(adherentId == null
				? seanceServices.getCalendrier(dateDebut, dateFin, tribuUuid)
				: seanceServices.getCalendrierForAdherent(dateDebut, dateFin, adherentId));
	}

	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "successful operation",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendrierGoogleResponse.class))
			),
	})
	@GetMapping("/calendrier/google")
	public ResponseEntity<CalendrierGoogleResponse> getCalendrierGoogle(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
			@RequestParam("source") List<String> sources) {
		return ResponseEntity.ok(googleAgendaServices.getCalendrier(dateDebut, dateFin, sources));
	}


	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "successful operation",
					content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SeanceResponse.class)))
			),
	})
	@GetMapping("/{activiteId}/seances")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<List<SeanceResponse>> getSeances(@PathVariable Long activiteId) {
		return ResponseEntity.ok(activiteServices.getSeances(activiteId));
	}

	@GetMapping("/managed/{activiteId}/seances")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<SeanceResponse>> getManagedSeances(Authentication principal, @PathVariable Long activiteId) {
		return ResponseEntity.ok(activiteServices.getManagedSeances(principal.getName(), activiteId));
	}

	@GetMapping("/{activiteId}/referents/candidats")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getReferentsCandidates(@PathVariable Long activiteId) {
		return ResponseEntity.ok(activiteServices.getReferentsCandidates(activiteId));
	}

	// Candidates can be registered in any activity belonging to the session's section.
	@GetMapping("/{activiteId}/responsabilites/candidats")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<List<AdherentResponsabiliteResponse>> getResponsabiliteCandidates(@PathVariable Long activiteId) {
		return ResponseEntity.ok(responsabiliteSeanceServices.getCandidats(activiteId));
	}

	@GetMapping("/managed/{activiteId}/responsabilites/candidats")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<AdherentResponsabiliteResponse>> getManagedResponsabiliteCandidates(
			Authentication principal, @PathVariable Long activiteId) {
		return ResponseEntity.ok(activiteServices.getManagedResponsabiliteCandidates(principal.getName(), activiteId));
	}

	@PostMapping("/{activiteId}/seances")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<?> addSeances(@PathVariable Long activiteId,
			@RequestBody @Valid AjoutSeancesRequest request) {
		return ResponseEntity.ok(activiteServices.addSeances(
				activiteId, request.nombreSeances(), request.dateDebut()));
	}

	@PostMapping("/{activiteId}/planifications/{planificationId}/seances")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<?> addSeancesPlanifiees(@PathVariable Long activiteId,
			@PathVariable Long planificationId, @RequestBody @Valid AjoutSeancesRequest request) {
		return ResponseEntity.ok(activiteServices.addSeances(
				activiteId, planificationId, request.nombreSeances(), request.dateDebut()));
	}

	@PostMapping("/managed/{activiteId}/planifications/{planificationId}/seances")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> addManagedSeancesPlanifiees(Authentication principal, @PathVariable Long activiteId,
			@PathVariable Long planificationId, @RequestBody @Valid AjoutSeancesRequest request) {
		return ResponseEntity.ok(activiteServices.addSeancesManaged(principal.getName(), activiteId,
				planificationId, request.nombreSeances(), request.dateDebut()));
	}

	@PatchMapping("/{activiteId}/seances/{seanceId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<?> updateSeance(@PathVariable Long activiteId,
			@PathVariable Long seanceId,
			@RequestBody @Valid MiseAJourSeanceRequest request) {
		return ResponseEntity.ok(SeanceResponse.from(seanceServices.updateSeance(
				activiteId, seanceId, request.etatSeance(), request.commentaire(), Boolean.TRUE.equals(request.commentairePresent()),
				request.date(), request.heureDebut(), Boolean.TRUE.equals(request.horairePresent()),
				request.salleId(), Boolean.TRUE.equals(request.sallePresente()), request.responsabilites())));
	}

	@PatchMapping("/managed/{activiteId}/seances/{seanceId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<SeanceResponse> updateManagedSeance(Authentication principal, @PathVariable Long activiteId,
			@PathVariable Long seanceId, @RequestBody @Valid MiseAJourSeanceRequest request) {
		return ResponseEntity.ok(activiteServices.updateManagedSeance(principal.getName(), activiteId, seanceId, request));
	}

	@DeleteMapping("/{activiteId}/seances/{seanceId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('SECRETAIRE')")
	public ResponseEntity<Void> deleteSeance(@PathVariable Long activiteId, @PathVariable Long seanceId) {
		seanceServices.deleteSeance(activiteId, seanceId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/managed/{activiteId}/seances/{seanceId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteManagedSeance(Authentication principal, @PathVariable Long activiteId,
			@PathVariable Long seanceId) {
		activiteServices.deleteManagedSeance(principal.getName(), activiteId, seanceId);
		return ResponseEntity.noContent().build();
	}


	@PostMapping("/save")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> save(@RequestBody Activite activite) {
		return ResponseEntity.ok(activiteServices.save(activite));
	}

	@PostMapping("/managed/save")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> saveManaged(Authentication principal, @RequestBody Activite activite) {
		return ResponseEntity.ok(activiteServices.saveManaged(principal.getName(), activite));
	}

	@GetMapping("/refillSeance")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> refillSeance(@RequestParam(value="activiteId") Long activiteId) {
		Activite activiteInDB = activiteServices.getById(activiteId);
		seanceServices.fillSeances(activiteInDB, 29);
		return ResponseEntity.ok(activiteInDB);
	}


	@PostMapping("/addReferent")
	@PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
	public ResponseEntity<?> save( @RequestParam(value="activiteId") Long activiteId, @RequestParam(value="adherentId") Long adherentId ) {
		return ResponseEntity.ok(activiteServices.addReferent(activiteId, adherentId));
	}


}
