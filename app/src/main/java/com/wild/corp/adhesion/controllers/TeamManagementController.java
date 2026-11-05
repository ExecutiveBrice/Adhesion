package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.services.TeamManagementService;
import com.wild.corp.adhesion.services.TeamManagementService.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import java.util.List;

@RestController @RequestMapping("/teams") @PreAuthorize("isAuthenticated()")
public class TeamManagementController {
    private final TeamManagementService service;
    public TeamManagementController(TeamManagementService service) { this.service = service; }
    public record Selection(List<Long> adhesions) { }
    public record Answer(Boolean value) { }
    public record Reminder(String requestId) { }
    @GetMapping("/mine") public List<TeamView> mine(Principal p) { return service.mine(p.getName()); }
    @GetMapping("/activities/{id}") public State state(@PathVariable Long id, Principal p) { return service.state(id, p.getName()); }

    @PostMapping("/activities/{id}/equipes") public TeamView create(@PathVariable Long id, @RequestBody TeamEdit body, Principal p) {
        return service.saveTeam(id, null, body, p.getName());
    }
    @PutMapping("/activities/{id}/equipes/{teamId}") public TeamView update(@PathVariable Long id, @PathVariable Long teamId, @RequestBody TeamEdit body, Principal p) {
        return service.saveTeam(id, teamId, body, p.getName());
    }
    @DeleteMapping("/activities/{id}/equipes/{teamId}") public void archive(@PathVariable Long id, @PathVariable Long teamId, Principal p) {
        service.archive(id, teamId, p.getName());
    }
    @PutMapping("/activities/{id}/equipes/{teamId}/effectif") public TeamView roster(@PathVariable Long id, @PathVariable Long teamId, @RequestBody RosterEdit body, Principal p) {
        return service.roster(id, teamId, body, p.getName());
    }
    @GetMapping("/equipes/{teamId}/events") public List<EventView> events(@PathVariable Long teamId, Principal p) { return service.events(teamId, p.getName()); }
    @PostMapping("/equipes/{teamId}/events") public EventView createEvent(@PathVariable Long teamId, @RequestBody EventEdit body, Principal p) { return service.createEvent(teamId, body, p.getName()); }
    @GetMapping("/events/{id}/candidates") public List<Person> candidates(@PathVariable Long id, Principal p) { return service.eventCandidates(id, p.getName()); }
    @PutMapping("/events/{id}/selection") public EventView select(@PathVariable Long id, @RequestBody Selection body, Principal p) { return service.select(id, body.adhesions(), p.getName()); }
    @PostMapping("/events/{id}/publish") public EventView publish(@PathVariable Long id, Principal p) { return service.publish(id, p.getName()); }
    @PostMapping("/events/{id}/remind") public EventView remind(@PathVariable Long id, @RequestBody Reminder body, Principal p) { return service.remind(id, body.requestId(), p.getName()); }
    @PostMapping("/events/{id}/cancel") public EventView cancel(@PathVariable Long id, Principal p) { return service.cancel(id, p.getName()); }
    @PutMapping("/events/{id}") public EventView reschedule(@PathVariable Long id, @RequestBody EventEdit body, Principal p) { return service.reschedule(id, body, p.getName()); }
    @GetMapping("/events/{id}/responsibility-candidates") public List<com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse> responsibilityCandidates(@PathVariable Long id, Principal p) {
        return service.responsibilityCandidates(id, p.getName());
    }
    @PutMapping("/events/{id}/responsibilities") public EventView responsibilities(@PathVariable Long id,
            @RequestBody List<com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest> body, Principal p) {
        return service.responsibilities(id, body, p.getName());
    }
    @GetMapping("/events/{id}/responsibility-teams") public List<com.wild.corp.adhesion.services.ResponsabiliteSeanceServices.EquipeCandidate> responsibilityTeams(@PathVariable Long id, Principal p) {
        return service.responsibilityTeams(id, p.getName());
    }
    @PatchMapping("/events/{id}/invitations/{invitationId}/reply") public EventView reply(@PathVariable Long id, @PathVariable Long invitationId, @RequestBody Answer body, Principal p) {
        required(body); return service.reply(id, invitationId, body.value(), p.getName());
    }
    @PatchMapping("/events/{id}/invitations/{invitationId}/attendance") public EventView attendance(@PathVariable Long id, @PathVariable Long invitationId, @RequestBody Answer body, Principal p) {
        required(body); return service.attendance(id, invitationId, body.value(), p.getName());
    }
    private void required(Answer a) { if (a.value() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réponse obligatoire"); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> businessError(ResponseStatusException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).body(detail);
    }
}
