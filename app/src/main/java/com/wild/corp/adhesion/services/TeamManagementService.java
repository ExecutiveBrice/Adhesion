package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional
public class TeamManagementService {
    private final ActiviteRepository activities;
    private final EquipeRepository teams;
    private final AdherentRepository people;
    private final UserRepository users;
    private final SectionRepository sections;
    private final SalleRepository rooms;
    private final SeanceRepository sessions;
    private final ConvocationEquipeRepository invitations;
    private final TeamMailRepository mails;
    private final PresenceServices presenceService;
    private final String frontend;
    private final ActivityTeamService activityTeams;
    private final ResponsabiliteSeanceServices responsibilities;

    public TeamManagementService(ActiviteRepository activities, EquipeRepository teams,
            AdherentRepository people, UserRepository users, SectionRepository sections, SalleRepository rooms,
            SeanceRepository sessions, ConvocationEquipeRepository invitations, TeamMailRepository mails,
            PresenceServices presenceService, ActivityTeamService activityTeams, ResponsabiliteSeanceServices responsibilities,
            @Value("${adhesion.frontend.base-url:http://localhost:4200}") String frontend) {
        this.activities = activities; this.teams = teams; this.people = people;
        this.users = users; this.sections = sections; this.rooms = rooms; this.sessions = sessions;
        this.invitations = invitations; this.mails = mails;
        this.presenceService = presenceService; this.frontend = frontend; this.activityTeams = activityTeams;
        this.responsibilities = responsibilities;
    }

    public record Person(Long id, String prenom, String nom, Long adhesionId, String statut) { }
    public record TeamView(Long id, Long activiteId, String activiteNom, String nom, String description,
                           boolean archivee, boolean canManage, List<Long> encadrants, List<Long> referents,
                           List<Long> effectif, int membres, boolean principale) { }
    public record State(List<TeamView> equipes, List<Person> candidats, List<Person> intervenants) { }
    public record TeamEdit(String nom, String description, List<Long> encadrants, List<Long> referents) { }
    public record RosterEdit(List<Long> adhesions, boolean applyToFuture) { }
    public record EventEdit(List<Long> equipes, String type, LocalDateTime debut, LocalDateTime fin,
                            Long salleId, String lieu, LocalDateTime rendezVous, String adversaire, String commentaire) { }
    public record InviteView(Long id, Long adherentId, Long adhesionId, String nom, boolean selectionnee,
                              LocalDateTime publication, Boolean reponse, LocalDateTime dateReponse,
                              Boolean presence, boolean canReply) { }
    public record EventView(Long id, String type, LocalDateTime debut, LocalDateTime fin, String etat,
                             String lieu, Long salleId, LocalDateTime rendezVous, String adversaire, String commentaire,
                             List<String> equipes, boolean canManage, List<InviteView> convocations,
                             Map<String, Long> emails, List<ResponsabiliteSeance> responsabilites) { }

    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette équipe ne vous est pas confiée"); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private User actor(String name) { return users.findByUsername(name).orElseThrow(TeamManagementService::forbidden); }
    private boolean manages(User user, Activite a) {
        return user.getRoles().contains(ERole.ROLE_ADMIN) || (a.getSection() != null
                && sections.findManagedByUsername(user.getUsername()).stream().anyMatch(s -> s.getId().equals(a.getSection().getId())));
    }
    private boolean onStaff(User u, Equipe t) {
        return java.util.stream.Stream.concat(t.getEncadrants().stream(), t.getReferents().stream())
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(u.getId()));
    }
    private boolean represents(User u, Adherent p) {
        return (p.getUser() != null && p.getUser().getId().equals(u.getId()))
                || (p.getRepresentant() != null && p.getRepresentant().getUser() != null
                && p.getRepresentant().getUser().getId().equals(u.getId()));
    }
    private boolean reads(User u, Equipe t) {
        return manages(u, t.getActivite()) || onStaff(u, t) || t.getAffectations().stream()
                .anyMatch(a -> TeamRoster.active(a, LocalDate.now()) && TeamRoster.eligible(a.getAdhesion(), t.getActivite())
                        && represents(u, a.getAdhesion().getAdherent()));
    }
    private Activite activity(Long id, User u, boolean write) {
        Activite a = (write ? activities.findForTeamUpdate(id) : activities.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
        if (!manages(u, a)) throw forbidden();
        return a;
    }
    private Equipe team(Long activityId, Long id) {
        return teams.findById(id).filter(t -> t.getActivite().getId().equals(activityId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipe introuvable dans cette activité"));
    }
    private Person person(Adherent p, Adhesion a) {
        return new Person(p.getId(), p.getPrenom(), p.getNom(), a == null ? null : a.getId(), a == null ? null : a.getStatutActuel());
    }
    private List<Adhesion> candidates(Activite a) {
        Map<Long, Adhesion> unique = new LinkedHashMap<>();
        java.util.stream.Stream.concat(a.getAdhesions().stream(), a.getSousClassement().stream())
                .filter(h -> TeamRoster.eligible(h, a)).sorted(Comparator.comparing(Adhesion::getId))
                .forEach(h -> unique.putIfAbsent(h.getAdherent().getId(), h));
        return List.copyOf(unique.values());
    }
    private List<Long> ids(Collection<Adherent> p) { return p.stream().map(Adherent::getId).sorted().toList(); }
    private TeamView view(Equipe t, User u) {
        boolean manager = manages(u, t.getActivite()) || onStaff(u, t);
        List<Long> roster = t.getAffectations().stream().filter(a -> TeamRoster.active(a, LocalDate.now()))
                .filter(a -> TeamRoster.eligible(a.getAdhesion(), t.getActivite()))
                .map(a -> a.getAdhesion().getId()).distinct().toList();
        return new TeamView(t.getId(), t.getActivite().getId(), t.getActivite().getNom(), t.getNom(), t.getDescription(),
                t.isArchivee(), manager && !t.isArchivee(), manager ? ids(t.getEncadrants()) : List.of(), manager ? ids(t.getReferents()) : List.of(),
                manager ? roster : List.of(), roster.size(),
                teams.findByActivite_IdOrderByIdAsc(t.getActivite().getId()).stream().filter(e -> !e.isArchivee()).findFirst().map(e -> e.getId().equals(t.getId())).orElse(false));
    }

    public TeamView saveTeam(Long activityId, Long teamId, TeamEdit edit, String name) {
        User u = actor(name); Activite a = activity(activityId, u, true);
        if (edit == null || edit.nom() == null || edit.nom().isBlank() || edit.nom().trim().length() > 100
                || (edit.description() != null && edit.description().length() > 500)) throw bad("Nom d'équipe requis (100 caractères maximum)");
        Equipe t = teamId == null ? new Equipe() : team(activityId, teamId);
        if (t.isArchivee()) throw conflict("Cette équipe est archivée");
        if (teams.findByActivite_IdOrderByNomAsc(activityId).stream().anyMatch(other -> !Objects.equals(other.getId(), teamId)
                && other.getNom().equalsIgnoreCase(edit.nom().trim()))) throw conflict("Ce nom d'équipe est déjà utilisé");
        Set<Long> allowedStaff = state(activityId, name).intervenants().stream().map(Person::id).collect(Collectors.toSet());
        t.setActivite(a); t.setNom(edit.nom().trim()); t.setDescription(edit.description());
        t.setEncadrants(resolveStaff(edit.encadrants(), allowedStaff)); t.setReferents(resolveStaff(edit.referents(), allowedStaff));
        teams.saveAndFlush(t);
        return view(t, u);
    }
    private Set<Adherent> resolveStaff(List<Long> ids, Set<Long> allowed) {
        if (ids == null) return new HashSet<>();
        if (ids.stream().anyMatch(Objects::isNull) || !allowed.containsAll(ids)) throw bad("Un intervenant n'est pas autorisé pour cette activité");
        return new HashSet<>(people.findAllById(ids));
    }

    public void archive(Long activityId, Long teamId, String name) {
        User u = actor(name); Activite a = activity(activityId, u, true); Equipe t = team(activityId, teamId);
        if (sessions.findByActivite_IdOrderByDebutAsc(activityId).stream().anyMatch(s -> s.getEquipes().contains(t)
                && s.getDebut().isAfter(LocalDateTime.now()) && s.getEtatSeance() != ESeance.ANNULEE)) {
            throw conflict("Annulez ou terminez les événements futures de cette équipe avant de l'archiver");
        }
        if (activityTeams.ensureFirstTeam(a).getId().equals(t.getId())) throw conflict("La première équipe de l’activité doit rester active");
        t.setArchivee(true);

    }

    public TeamView roster(Long activityId, Long teamId, RosterEdit edit, String name) {
        User u = actor(name); Activite a = activity(activityId, u, true); Equipe t = team(activityId, teamId);
        if (t.isArchivee()) throw conflict("Cette équipe est archivée");
        if (edit == null || edit.adhesions() == null || edit.adhesions().stream().anyMatch(Objects::isNull)) throw bad("Effectif invalide");
        Map<Long, Adhesion> allowed = candidates(a).stream().collect(Collectors.toMap(Adhesion::getId, h -> h));
        Set<Long> selected = new HashSet<>(edit.adhesions());
        if (!allowed.keySet().containsAll(selected)) throw bad("Un adhérent n'est pas inscrit ou surclassé dans cette activité");
        LocalDate today = LocalDate.now();
        t.getAffectations().stream().filter(h -> TeamRoster.active(h, today) && !selected.contains(h.getAdhesion().getId()))
                .forEach(h -> h.setFin(today));
        Set<Long> current = t.getAffectations().stream().filter(h -> TeamRoster.active(h, today))
                .map(h -> h.getAdhesion().getId()).collect(Collectors.toSet());
        selected.stream().filter(h -> !current.contains(h)).forEach(id -> {
            AffectationEquipe h = new AffectationEquipe(); h.setEquipe(t); h.setAdhesion(allowed.get(id)); h.setDebut(today);
            t.getAffectations().add(h);
        });
        teams.saveAndFlush(t);
        if (edit.applyToFuture()) sessions.findByActivite_IdOrderByDebutAsc(activityId).stream()
                .filter(s -> s.getEquipes().contains(t) && s.getDebut().isAfter(LocalDateTime.now())
                        && s.getEtatSeance() != ESeance.ANNULEE)
                .filter(s -> invitations.findBySeance_IdOrderByIdAsc(s.getId()).stream().noneMatch(c -> c.getPublication() != null))
                .filter(s -> s.getPresences().stream().noneMatch(p -> p.getPresence() != null || p.getPresencePrevue() != null))
                .forEach(s -> {
                    s.getConvocationsEquipe().clear();
                    s.getPresences().clear(); sessions.flush(); presenceService.fillPresences(s);
                    ensureDrafts(s);
                });
        return view(t, u);
    }

    private String text(String s, int max) { if (s != null && s.length() > max) throw bad("Texte trop long (" + max + " caractères maximum)"); return s; }
    private Salle room(Long id) { return id == null ? null : rooms.findById(id).orElseThrow(() -> bad("Salle introuvable")); }
    private Set<Equipe> targets(Activite a, List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) throw bad("Sélectionnez au moins une équipe");
        Set<Equipe> target = ids.stream().map(id -> team(a.getId(), id)).collect(Collectors.toSet());
        if (target.stream().anyMatch(Equipe::isArchivee)) throw bad("Une équipe est archivée");
        return target;
    }

    public List<TeamView> mine(String name) {
        User u = actor(name);
        Set<Long> invitedTeams = invitations.findPublishedForUser(name).stream().flatMap(c -> c.getSeance().getEquipes().stream())
                .map(Equipe::getId).collect(Collectors.toSet());
        return teams.findAll().stream().filter(t -> reads(u, t) || invitedTeams.contains(t.getId()))
                .sorted(Comparator.comparing(Equipe::getNom)).map(t -> view(t, u)).toList();
    }
    private Seance event(Long id, User u, boolean write) {
        Seance s = sessions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));
        if (write) {
            activities.findForTeamUpdate(s.getActivite().getId()).orElseThrow();
           
            if (!managesSession(u, s)) throw forbidden();
        } else if (!managesSession(u, s) && s.getEquipes().stream().noneMatch(t -> reads(u, t))
                && invitations.findBySeance_IdOrderByIdAsc(id).stream().noneMatch(c -> c.getPublication() != null && c.isSelectionnee() && represents(u, c.getAdherent()))) {
            throw forbidden();
        }
        return s;
    }

    public List<EventView> events(Long teamId, String name) {
        User u = actor(name); Equipe t = teams.findById(teamId).orElseThrow(() -> bad("Équipe introuvable"));
        boolean fullRead = reads(u, t);
        Set<Long> ownEvents = invitations.findPublishedForUser(name).stream().map(c -> c.getSeance().getId()).collect(Collectors.toSet());
        if (!fullRead && invitations.findPublishedForUser(name).stream().noneMatch(c -> c.getSeance().getEquipes().contains(t))) throw forbidden();
        return sessions.findByActivite_IdOrderByDebutAsc(t.getActivite().getId()).stream()
                .filter(s -> s.getEquipes().contains(t) && (fullRead || ownEvents.contains(s.getId())))
                .map(s -> eventView(s, u)).toList();
    }
    public List<Person> eventCandidates(Long sessionId, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true);
        return candidates(s.getActivite()).stream().map(h -> person(h.getAdherent(), h)).toList();
    }
    private EventView eventView(Seance s, User u) {
        boolean manager = managesSession(u, s);
        Map<Long, Boolean> actual = new HashMap<>();
        s.getPresences().forEach(p -> actual.put(p.getAdhesion().getAdherent().getId(), p.getPresence()));
        List<InviteView> list = invitations.findBySeance_IdOrderByIdAsc(s.getId()).stream()
                .filter(c -> manager || (c.isSelectionnee() && c.getPublication() != null && represents(u, c.getAdherent())))
                .map(c -> new InviteView(c.getId(), c.getAdherent().getId(), c.getAdhesion().getId(),
                        c.getAdherent().getPrenom() + " " + c.getAdherent().getNom(), c.isSelectionnee(), c.getPublication(),
                        c.getReponse(), c.getDateReponse(), actual.get(c.getAdherent().getId()),
                        c.isSelectionnee() && c.getPublication() != null && represents(u, c.getAdherent())
                                && s.getDebut().isAfter(LocalDateTime.now()) && s.getEtatSeance() != ESeance.ANNULEE
                                && TeamRoster.eligible(c.getAdhesion(), s.getActivite()))).toList();
        Map<String, Long> mailStates = manager ? mails.findBySeance_Id(s.getId()).stream()
                .collect(Collectors.groupingBy(TeamMail::getEtat, Collectors.counting())) : Map.of();
        return new EventView(s.getId(), s.getTypeEvenement(), s.getDebut(), s.getFin(), s.getEtatSeance().name(),
                s.getLieuExterieur() != null ? s.getLieuExterieur() : (s.getSalle() == null ? null : s.getSalle().getNom()),
                s.getSalle() == null ? null : s.getSalle().getId(), s.getRendezVous(), s.getAdversaire(), s.getCommentaire(), s.getEquipes().stream().map(Equipe::getNom).sorted().toList(),
                manager, list, mailStates, List.copyOf(s.getResponsabilites()));
    }
    public List<com.wild.corp.adhesion.models.resources.AdherentResponsabiliteResponse> responsibilityCandidates(Long id, String name) {
        Seance s = event(id, actor(name), true);
        return responsibilities.getCandidats(s.getActivite().getId());
    }
    public List<ResponsabiliteSeanceServices.EquipeCandidate> responsibilityTeams(Long id, String name) {
        return responsibilities.getEquipes(event(id, actor(name), true));
    }
    public EventView responsibilities(Long id, List<com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest> requests, String name) {
        if (requests == null) throw bad("Liste de responsabilités obligatoire");
        User u = actor(name); Seance s = event(id, u, true);
        responsibilities.remplacerParEquipes(s, requests);
        return eventView(s, u);
    }
    public EventView createEvent(Long teamId, EventEdit edit, String name) {
        if (edit == null) throw bad("Événement obligatoire");
        User u = actor(name); Equipe t = teams.findById(teamId).orElseThrow(() -> bad("Équipe introuvable"));
        activities.findForTeamUpdate(t.getActivite().getId()).orElseThrow();
        if (!manages(u, t.getActivite()) && !onStaff(u, t)) throw forbidden();
        if (t.isArchivee()) throw conflict("Cette équipe est archivée");
        Seance s = new Seance(); s.setActivite(t.getActivite());
        Set<Equipe> target = targets(t.getActivite(), edit.equipes());
        if (!target.contains(t) || (!manages(u, t.getActivite()) && target.stream().anyMatch(other -> !onStaff(u, other)))) throw forbidden();
        s.setEquipes(target); applyEvent(s, edit); s.setEtatSeance(ESeance.PROGRAMMEE);
        presenceService.fillPresences(s); sessions.saveAndFlush(s); ensureDrafts(s);
        return eventView(s, u);
    }
    void applyEvent(Seance s, EventEdit edit) {
        if (edit == null) throw bad("Événement obligatoire");
        if (edit.debut() == null) throw bad("Renseignez une date et une heure de début valides.");
        if (edit.fin() == null) throw bad("Renseignez une date et une heure de fin valides.");
        if (!edit.fin().isAfter(edit.debut())) throw bad("La fin de l’événement doit être postérieure à son début.");
        if (Duration.between(edit.debut(), edit.fin()).compareTo(Duration.ofHours(72)) > 0) throw bad("La durée de l’événement ne peut pas dépasser 72 heures.");
        if (edit.type() == null || !List.of("ACTIVITE", "RENCONTRE", "TOURNOI", "AUTRE").contains(edit.type())) throw bad("Sélectionnez un type d’événement valide.");
        if (edit.rendezVous() != null && edit.rendezVous().isAfter(edit.debut())) throw bad("Le rendez-vous ne peut pas être postérieur au début de l’événement.");
        s.setDebut(edit.debut()); s.setFin(edit.fin()); s.setTypeEvenement(edit.type()); s.setSalle(room(edit.salleId()));
        s.setLieuExterieur(text(edit.lieu(), 200)); s.setAdversaire(text(edit.adversaire(), 100));
        s.setCommentaire(text(edit.commentaire(), 2000)); s.setRendezVous(edit.rendezVous() == null ? edit.debut().minusHours(1) : edit.rendezVous());
    }
    private void ensureDrafts(Seance s) {
        Set<Long> existing = invitations.findBySeance_IdOrderByIdAsc(s.getId()).stream().map(c -> c.getAdherent().getId()).collect(Collectors.toSet());
        s.getPresences().stream().filter(p -> !existing.contains(p.getAdhesion().getAdherent().getId())).forEach(p -> {
            ConvocationEquipe c = new ConvocationEquipe(); c.setSeance(s); c.setAdhesion(p.getAdhesion());
            c.setAdherent(p.getAdhesion().getAdherent()); c.setSelectionnee(true); s.getConvocationsEquipe().add(c); invitations.save(c);
        });
        invitations.flush();
    }

    public EventView select(Long sessionId, List<Long> adhesionIds, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true); requireUpcoming(s);
        if (adhesionIds == null || adhesionIds.stream().anyMatch(Objects::isNull)) throw bad("Sélection invalide");
        Map<Long, Adhesion> allowed = candidates(s.getActivite()).stream().collect(Collectors.toMap(Adhesion::getId, a -> a));
        if (!allowed.keySet().containsAll(adhesionIds)) throw bad("Un participant n'est pas autorisé dans cette activité");
        ensureDrafts(s);
        Map<Long, ConvocationEquipe> existing = invitations.findBySeance_IdOrderByIdAsc(sessionId).stream().collect(Collectors.toMap(c -> c.getAdherent().getId(), c -> c));
        Set<Long> selectedPeople = adhesionIds.stream().map(id -> allowed.get(id).getAdherent().getId()).collect(Collectors.toSet());
        for (ConvocationEquipe c : existing.values()) {
            if (c.isSelectionnee() && !selectedPeople.contains(c.getAdherent().getId())) {
                c.setSelectionnee(false);
                if (c.getPublication() != null) queue(s, List.of(c), "RETRAIT", "retire-" + c.getId() + "-" + c.getPublication(), true);
            }
        }
        for (Long id : new LinkedHashSet<>(adhesionIds)) {
            Adhesion a = allowed.get(id); ConvocationEquipe c = existing.get(a.getAdherent().getId());
            if (c == null) {
                c = new ConvocationEquipe(); c.setSeance(s); c.setAdherent(a.getAdherent()); c.setAdhesion(a);
                s.getConvocationsEquipe().add(c); invitations.save(c); existing.put(a.getAdherent().getId(), c);
            }
            if (!c.isSelectionnee()) { c.setPublication(null); c.setReponse(null); c.setDateReponse(null); c.setReponduPar(null); }
            c.setSelectionnee(true);
        }
        invitations.flush(); return eventView(s, u);
    }
    private void requireUpcoming(Seance s) {
        if (s.getEtatSeance() == ESeance.ANNULEE || !s.getDebut().isAfter(LocalDateTime.now())) throw conflict("L'événement doit être à venir et non annulé");
    }
    public EventView publish(Long sessionId, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true); requireUpcoming(s); ensureDrafts(s);
        List<ConvocationEquipe> newly = invitations.findBySeance_IdOrderByIdAsc(sessionId).stream()
                .filter(c -> c.isSelectionnee() && c.getPublication() == null).toList();
        if (newly.stream().anyMatch(c -> !TeamRoster.eligible(c.getAdhesion(), s.getActivite()))) throw conflict("Un dossier a changé : vérifiez la sélection");
        for (ConvocationEquipe c : newly) {
            c.setPublication(LocalDateTime.now());
            if (s.getPresences().stream().noneMatch(p -> p.getAdhesion().getAdherent().getId().equals(c.getAdherent().getId()))) {
                Presence p = new Presence(); p.setSeance(s); p.setAdhesion(c.getAdhesion()); s.getPresences().add(p);
            }
        }
        queue(s, newly, "CONVOCATION", "publication", false);
        return eventView(s, u);
    }
    public EventView remind(Long sessionId, String requestId, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true); requireUpcoming(s);
        try { UUID.fromString(requestId); } catch (Exception ex) { throw bad("Identifiant de relance invalide"); }
        queue(s, invitations.findBySeance_IdOrderByIdAsc(sessionId).stream()
                .filter(c -> c.isSelectionnee() && c.getPublication() != null && c.getReponse() == null)
                .filter(c -> TeamRoster.eligible(c.getAdhesion(), s.getActivite())).toList(), "RELANCE", requestId, false);
        return eventView(s, u);
    }
    public EventView cancel(Long sessionId, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true);
        if (s.getEtatSeance() != ESeance.ANNULEE) {
            requireUpcoming(s); s.setEtatSeance(ESeance.ANNULEE); s.setTeamRevision(s.getTeamRevision() + 1);
            queue(s, invitations.findBySeance_IdOrderByIdAsc(sessionId).stream().filter(c -> c.isSelectionnee() && c.getPublication() != null).toList(), "ANNULATION", "annulation", true);
        }
        return eventView(s, u);
    }
    public EventView reschedule(Long sessionId, EventEdit edit, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true); requireUpcoming(s);
        applyEvent(s, edit); requireUpcoming(s); s.setTeamRevision(s.getTeamRevision() + 1); s.setEtatSeance(ESeance.MODIFIEE);
        List<ConvocationEquipe> published = invitations.findBySeance_IdOrderByIdAsc(sessionId).stream()
                .filter(c -> c.isSelectionnee() && c.getPublication() != null).toList();
        published.forEach(c -> { c.setReponse(null); c.setDateReponse(null); c.setReponduPar(null); });
        queue(s, published, "MODIFICATION", "report", false); return eventView(s, u);
    }
    public EventView reply(Long sessionId, Long invitationId, boolean response, String name) {
        User u = actor(name); Seance s = event(sessionId, u, false);
        activities.findForTeamUpdate(s.getActivite().getId()).orElseThrow(); requireUpcoming(s);
        ConvocationEquipe c = invitations.findById(invitationId).filter(i -> i.getSeance().getId().equals(sessionId)
                && i.isSelectionnee() && i.getPublication() != null).orElseThrow(TeamManagementService::forbidden);
        if (!represents(u, c.getAdherent()) || !TeamRoster.eligible(c.getAdhesion(), s.getActivite())) throw forbidden();
        c.setReponse(response); c.setDateReponse(LocalDateTime.now()); c.setReponduPar(u.getId());
        c.getHistorique().add(new ReponseConvocation(c.getDateReponse(), response, u.getId(), s.getTeamRevision()));
        return eventView(s, u);
    }
    public EventView attendance(Long sessionId, Long invitationId, boolean present, String name) {
        User u = actor(name); Seance s = event(sessionId, u, true);
        if (s.getEtatSeance() == ESeance.ANNULEE) throw conflict("Événement annulé");
        ConvocationEquipe c = invitations.findById(invitationId).filter(i -> i.getSeance().getId().equals(sessionId) && i.isSelectionnee())
                .orElseThrow(() -> bad("Participant introuvable"));
        Presence p = s.getPresences().stream().filter(v -> v.getAdhesion().getAdherent().getId().equals(c.getAdherent().getId())).findFirst().orElse(null);
        if (p == null) { p = new Presence(); p.setSeance(s); p.setAdhesion(c.getAdhesion()); s.getPresences().add(p); }
        p.setPresence(present); p.setDateModification(LocalDate.now()); sessions.saveAndFlush(s); return eventView(s, u);
    }

    private void queue(Seance s, List<ConvocationEquipe> recipients, String kind, String command, boolean cancellation) {
        Map<String, List<ConvocationEquipe>> grouped = recipients.stream().collect(Collectors.groupingBy(c -> address(c.getAdherent())));
        grouped.forEach((address, list) -> {
            String raw = s.getId() + ":" + s.getTeamRevision() + ":" + kind + ":" + command + ":" + address + ":"
                    + list.stream().sorted(Comparator.comparing(ConvocationEquipe::getId))
                        .map(c -> c.getId() + "@" + c.getPublication()).collect(Collectors.joining(","));
            String key;
            try { key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
            if (mails.existsByCle(key)) return;
            TeamMail mail = new TeamMail(); mail.setSeance(s); mail.setCle(key); mail.setRevision(s.getTeamRevision());
            mail.setType(kind);
            mail.getConvocations().addAll(list.stream().map(ConvocationEquipe::getId).toList());
            mail.setDestinataire(address); mail.setAnnulation(cancellation); mail.setSujet(kind + " · " + s.getActivite().getNom());
            mail.setContenu("<p>" + html(kind) + " : " + html(s.getActivite().getNom()) + "</p><p>"
                    + list.stream().map(c -> html(c.getAdherent().getPrenom() + " " + c.getAdherent().getNom())).collect(Collectors.joining(", "))
                    + "</p><p>" + html(s.getDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    + "</p><p>" + html(s.getLieuExterieur() != null ? s.getLieuExterieur() : (s.getSalle() == null ? "" : s.getSalle().getNom()))
                    + "</p><p><a href=\"" + html(frontend.replaceAll("/$", "") + "/equipes") + "\">Consulter Mes équipes et répondre</a></p>");
            if (address.isBlank() || address.endsWith("@sans-email.invalid") || !address.contains("@")) mail.setEtat("NO_ADDRESS");
            mails.save(mail);
        });
    }
    private String address(Adherent p) {
        return TeamRoster.contactAddress(p);
    }
    private String html(String s) { return HtmlUtils.htmlEscape(s == null ? "" : s); }
    public State state(Long id, String name) {
        User u = actor(name); Activite a = activity(id, u, false);
        Map<Long, Adherent> staff = new LinkedHashMap<>();
        people.findByUserRole(ERole.ROLE_ENCADRANT).forEach(p -> staff.put(p.getId(), p));
        candidates(a).forEach(h -> {
            Adherent p = h.getAdherent(); staff.put(p.getId(), p);
            if (p.getRepresentant() != null) staff.put(p.getRepresentant().getId(), p.getRepresentant());
        });
        teams.findByActivite_IdOrderByIdAsc(id).forEach(t -> java.util.stream.Stream.concat(t.getEncadrants().stream(), t.getReferents().stream())
                .forEach(p -> staff.put(p.getId(), p)));
        return new State(teams.findByActivite_IdOrderByIdAsc(id).stream().map(t -> view(t, u)).toList(),
                candidates(a).stream().map(h -> person(h.getAdherent(), h)).toList(),
                staff.values().stream().filter(p -> p.getUser() != null).map(p -> person(p, null))
                    .sorted(Comparator.comparing(Person::nom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Person::prenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList());
    }

    private boolean managesSession(User u, Seance s) {
        return manages(u, s.getActivite()) || s.getEquipes().stream().anyMatch(t -> onStaff(u, t));
    }

}
