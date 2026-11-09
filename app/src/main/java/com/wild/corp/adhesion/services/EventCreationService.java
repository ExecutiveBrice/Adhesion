package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import com.wild.corp.adhesion.services.TeamManagementService.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service @Transactional
public class EventCreationService {
    private final TeamManagementService management;
    private final SectionRepository sections;
    private final EquipeRepository teams;
    private final UserRepository users;
    private final ActiviteRepository activities;
    private final ActivityTeamService activityTeams;
    private final EventCalendarService calendar;
    public EventCreationService(TeamManagementService management, SectionRepository sections, EquipeRepository teams,
            UserRepository users, ActiviteRepository activities, ActivityTeamService activityTeams, EventCalendarService calendar) {
        this.management = management; this.sections = sections; this.teams = teams; this.users = users;
        this.activities = activities; this.activityTeams = activityTeams; this.calendar = calendar;
    }
    public record SectionTarget(Long id, String nom) { }
    public record Options(List<TeamView> equipes, List<SectionTarget> sections) { }
    public record Creation(EventEdit evenement, List<Long> sections, Integer semaines) { }
    public record Created(int nombre, List<EventView> evenements) { }
    private List<Section> managedSections(String name) {
        User user = users.findByUsername(name).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        return user.getRoles().contains(ERole.ROLE_ADMIN) ? sections.findAllByOrderByNomAsc() : sections.findManagedByUsername(name);
    }
    public Options options(String name) {
        return new Options(management.managedTeams(name),
                managedSections(name).stream().map(s -> new SectionTarget(s.getId(), s.getNom())).toList());
    }
    public Created create(Creation request, String name) {
        if (request == null || request.evenement() == null) throw bad("Événement obligatoire");
        EventEdit edit = request.evenement();
        management.applyEvent(new Seance(), edit);
        int weeks = request.semaines() == null ? 1 : request.semaines();
        if (weeks < 1 || weeks > 104) throw bad("Le nombre de semaines doit être compris entre 1 et 104.");
        Set<Long> ids = new LinkedHashSet<>(edit.equipes() == null ? List.of() : edit.equipes());
        Set<Long> allowed = management.managedTeams(name).stream().map(TeamView::id).collect(java.util.stream.Collectors.toSet());
        if (!allowed.containsAll(ids)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Une équipe ne vous est pas confiée");
        Map<Long, Section> managed = new HashMap<>(); managedSections(name).forEach(s -> managed.put(s.getId(), s));
        for (Long id : new LinkedHashSet<>(request.sections() == null ? List.of() : request.sections())) {
            if (!managed.containsKey(id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Une section ne vous est pas confiée");
            List<Activite> sectionActivities = activities.findBySectionIdOrderByNomAsc(id);
            if (sectionActivities.isEmpty()) throw bad("La section « " + managed.get(id).getNom() + " » ne contient aucune activité.");
            for (Activite activity : sectionActivities) {
                activityTeams.ensureFirstTeam(activity);
                teams.findByActivite_IdOrderByIdAsc(activity.getId()).stream().filter(t -> !t.isArchivee()).forEach(t -> ids.add(t.getId()));
            }
        }
        if (ids.isEmpty()) throw bad("Sélectionnez au moins une équipe ou une section.");
        Map<Long, List<Long>> grouped = new TreeMap<>();
        for (Long id : ids) {
            Equipe team = teams.findById(id).orElseThrow(() -> bad("Équipe introuvable"));
            grouped.computeIfAbsent(team.getActivite().getId(), k -> new ArrayList<>()).add(id);
        }
        Set<LocalDate> excluded = weeks == 1 ? Set.of() : calendar.unavailable(edit.debut().toLocalDate(), edit.fin().plusWeeks(weeks - 1L).toLocalDate());
        List<EventView> created = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            LocalDateTime start = edit.debut().plusWeeks(week), end = edit.fin().plusWeeks(week);
            if (weeks > 1 && start.toLocalDate().datesUntil(end.toLocalDate().plusDays(end.toLocalTime().equals(LocalTime.MIDNIGHT) ? 0 : 1)).anyMatch(excluded::contains)) continue;
            for (List<Long> target : grouped.values()) {
                EventEdit occurrence = new EventEdit(target, edit.type(), start, end, edit.salleId(), edit.lieu(),
                        edit.rendezVous() == null ? start.minusHours(1) : edit.rendezVous().plusWeeks(week), edit.adversaire(), edit.commentaire());
                created.add(management.createEvent(target.getFirst(), occurrence, name));
            }
        }
        if (created.isEmpty()) throw bad("Toutes les dates choisies tombent pendant les vacances ou un jour férié.");
        return new Created(created.size(), created);
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
