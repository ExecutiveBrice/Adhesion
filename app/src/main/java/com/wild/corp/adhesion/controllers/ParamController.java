package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ParamBoolean;
import com.wild.corp.adhesion.models.ParamNumber;
import com.wild.corp.adhesion.models.ParamText;
import com.wild.corp.adhesion.models.resources.AgendaGoogleConfiguration;
import com.wild.corp.adhesion.models.resources.SalleConfiguration;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.models.resources.SectionConfiguration;
import com.wild.corp.adhesion.models.resources.ChatConfiguration;
import com.wild.corp.adhesion.models.resources.TacheSeanceConfiguration;
import com.wild.corp.adhesion.services.GoogleAgendaConfigurationServices;
import com.wild.corp.adhesion.services.ParamBooleanServices;
import com.wild.corp.adhesion.services.ParamNumberServices;
import com.wild.corp.adhesion.services.ParamTextServices;
import com.wild.corp.adhesion.services.SalleConfigurationServices;
import com.wild.corp.adhesion.services.SectionConfigurationServices;
import com.wild.corp.adhesion.services.ChatConfigurationServices;
import com.wild.corp.adhesion.services.TacheSeanceConfigurationServices;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

	@Autowired
	GoogleAgendaConfigurationServices googleAgendaConfigurationServices;

	@Autowired
	SalleConfigurationServices salleConfigurationServices;

	@Autowired
	SectionConfigurationServices sectionConfigurationServices;

	@Autowired
	ChatConfigurationServices chatConfigurationServices;

	@Autowired
	TacheSeanceConfigurationServices tacheSeanceConfigurationServices;

	@GetMapping("/taches-seance")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> getTachesSeance() {
		return ResponseEntity.ok(tacheSeanceConfigurationServices.getAll());
	}

	@PostMapping("/taches-seance")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createTacheSeance(@RequestBody TacheSeanceConfiguration tache) {
		return ResponseEntity.ok(tacheSeanceConfigurationServices.create(tache));
	}

	@PutMapping("/taches-seance/{tacheId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateTacheSeance(@PathVariable Long tacheId,
			@RequestBody TacheSeanceConfiguration tache) {
		return ResponseEntity.ok(tacheSeanceConfigurationServices.update(tacheId, tache));
	}

	@DeleteMapping("/taches-seance/{tacheId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteTacheSeance(@PathVariable Long tacheId) {
		tacheSeanceConfigurationServices.delete(tacheId);
		return ResponseEntity.noContent().build();
	}

	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "successful operation",
					content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AgendaGoogleConfiguration.class)))
			),
	})
	@GetMapping("/agendas")
	public ResponseEntity<List<AgendaGoogleConfiguration>> getAgendas() {
		return ResponseEntity.ok(googleAgendaConfigurationServices.getAll());
	}

	@PostMapping("/agendas")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createAgenda(@RequestBody AgendaGoogleConfiguration agenda) {
		return ResponseEntity.ok(googleAgendaConfigurationServices.create(agenda));
	}

	@PutMapping("/agendas/{agendaId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateAgenda(@PathVariable Long agendaId,
			@RequestBody AgendaGoogleConfiguration agenda) {
		return ResponseEntity.ok(googleAgendaConfigurationServices.update(agendaId, agenda));
	}

	@DeleteMapping("/agendas/{agendaId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteAgenda(@PathVariable Long agendaId) {
		googleAgendaConfigurationServices.delete(agendaId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/salles")
	public ResponseEntity<?> getSalles() {
		return ResponseEntity.ok(salleConfigurationServices.getAll());
	}

	@PostMapping("/salles")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createSalle(@RequestBody SalleConfiguration salle) {
		return ResponseEntity.ok(salleConfigurationServices.create(salle));
	}

	@PutMapping("/salles/{salleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateSalle(@PathVariable Long salleId, @RequestBody SalleConfiguration salle) {
		return ResponseEntity.ok(salleConfigurationServices.update(salleId, salle));
	}

	@DeleteMapping("/salles/{salleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteSalle(@PathVariable Long salleId) {
		salleConfigurationServices.delete(salleId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/sections")
	public ResponseEntity<List<SectionConfiguration>> getSections() {
		return ResponseEntity.ok(sectionConfigurationServices.getAll());
	}

	@PostMapping("/sections")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createSection(@RequestBody SectionConfiguration section) {
		return ResponseEntity.ok(sectionConfigurationServices.create(section));
	}

	@PutMapping("/sections/{sectionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateSection(@PathVariable Long sectionId,
			@RequestBody SectionConfiguration section) {
		return ResponseEntity.ok(sectionConfigurationServices.update(sectionId, section));
	}

	@GetMapping("/sections/{sectionId}/chats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<ChatConfiguration>> getChats(@PathVariable Long sectionId) {
		return ResponseEntity.ok(chatConfigurationServices.getAll(sectionId));
	}

	@PostMapping("/sections/{sectionId}/chats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createChat(@PathVariable Long sectionId,
			@RequestBody ChatConfiguration chat) {
		return ResponseEntity.ok(chatConfigurationServices.create(sectionChatFor(sectionId, chat)));
	}

	@PutMapping("/sections/{sectionId}/chats/{chatId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateChat(@PathVariable Long sectionId, @PathVariable Long chatId,
			@RequestBody ChatConfiguration chat) {
		return ResponseEntity.ok(chatConfigurationServices.update(sectionId, chatId, sectionChatFor(sectionId, chat)));
	}

	@DeleteMapping("/sections/{sectionId}/chats/{chatId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteChat(@PathVariable Long sectionId, @PathVariable Long chatId) {
		chatConfigurationServices.delete(sectionId, chatId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/chats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<ChatConfiguration>> getChats() {
		return ResponseEntity.ok(chatConfigurationServices.getAll());
	}

	@PostMapping("/chats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createChat(@RequestBody ChatConfiguration chat) {
		return ResponseEntity.ok(chatConfigurationServices.create(chat));
	}

	@PutMapping("/chats/{chatId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateChat(@PathVariable Long chatId, @RequestBody ChatConfiguration chat) {
		return ResponseEntity.ok(chatConfigurationServices.update(chatId, chat));
	}

	@DeleteMapping("/chats/{chatId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
		chatConfigurationServices.delete(chatId);
		return ResponseEntity.noContent().build();
	}

	private ChatConfiguration sectionChatFor(Long sectionId, ChatConfiguration chat) {
		return new ChatConfiguration(chat.id(), chat.nom(), "SECTION", sectionId, null, chat.permissions());
	}

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
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> notification(@RequestBody Object param) {
		log.error(param.toString());
		return ResponseEntity.ok(param.toString());
	}


	@GetMapping("/allNumber")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllNumber() {
		return ResponseEntity.ok(paramNumberServices.getAll());
	}

	@PostMapping("/saveNumber")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> saveNumber(@RequestBody ParamNumber param) {
		return ResponseEntity.ok(paramNumberServices.save(param));
	}
}
