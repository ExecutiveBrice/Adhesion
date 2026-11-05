package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.services.*;
import com.wild.corp.adhesion.services.TeamManagementService.*;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = { "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false" })
@ContextConfiguration(classes = TeamManagementIntegrationTest.Config.class)
@Import({EventCreationService.class, TeamManagementService.class, PresenceServices.class, ActivityTeamService.class, ResponsabiliteSeanceServices.class})
class TeamManagementIntegrationTest {
    @SpringBootConfiguration @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Seance.class) @EnableJpaRepositories(basePackageClasses = EquipeRepository.class)
    static class Config { }
    @Autowired EntityManager em;
    @Autowired TeamManagementService service;
    @Autowired EventCreationService creation;
    @org.springframework.test.context.bean.override.mockito.MockitoBean EventCalendarService calendar;

    @Autowired EquipeRepository teams;
    @Autowired SeanceRepository sessions;
    @Autowired ConvocationEquipeRepository invitations;
    @Autowired TeamMailRepository mails;
    @Autowired PresenceServices presences;
    @Autowired ActivityTeamService activityTeams;
    Activite activity;
    Adherent admin, parent, alice, bob, outsider, coach;
    Adhesion aliceMembership, bobMembership;
    LocalDateTime start;

    @BeforeEach void setup() {
        admin = person("admin", ERole.ROLE_ADMIN);
        parent = person("parent", ERole.ROLE_USER);
        alice = person("alice", ERole.ROLE_USER); alice.setRepresentant(parent); alice.setEmailRepresentant(true);
        bob = person("bob", ERole.ROLE_USER); bob.setRepresentant(parent); bob.setEmailRepresentant(true);
        outsider = person("outsider", ERole.ROLE_USER);
        coach = person("coach", ERole.ROLE_ENCADRANT);
        Section section = Section.builder().nom("Danse").type("NON_COMPETITIVE").build(); em.persist(section);
        activity = new Activite(); activity.setSection(section); activity.setNom("Danse adultes"); em.persist(activity);
        aliceMembership = membership(alice, activity); bobMembership = membership(bob, activity);
        start = LocalDate.now().plusDays(5).atTime(18, 0);
        em.flush();
    }
    Adherent person(String name, ERole role) {
        User user = new User(name + "@example.org", "password"); user.getRoles().add(role); em.persist(user);
        Adherent p = new Adherent(); p.setPrenom(name); p.setNom("Test"); p.setUser(user); user.setAdherent(p); em.persist(p); return p;
    }
    Adhesion membership(Adherent p, Activite a) {
        Adhesion h = new Adhesion(); h.setActivite(a); h.setAdherent(p); h.setStatutActuel(Status.VALIDEE.label);
        em.persist(h); a.getAdhesions().add(h); p.getAdhesions().add(h); return h;
    }
    String login(Adherent p) { return p.getUser().getUsername(); }
    TeamView team(String name, Long... memberIds) {

        TeamView t = service.saveTeam(activity.getId(), null, new TeamEdit(name, "", List.of(coach.getId()), List.of()), login(admin));
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(memberIds), false), login(admin)); return t;
    }
    EventEdit edit(List<Long> teamIds) {
        return new EventEdit(teamIds, "ACTIVITE", start, start.plusHours(1), null, "Studio", start.minusMinutes(15), "", "Venir en tenue");
    }
    EventView event(TeamView t) { return service.createEvent(t.id(), edit(List.of(t.id())), login(admin)); }

    @Test void recurringEventsSkipExcludedWeeksAndKeepLocalTimesAndMeetingOffset() {
        start = LocalDateTime.of(2027, 3, 20, 18, 0);
        TeamView t = team("Récurrence", aliceMembership.getId());
        org.mockito.Mockito.when(calendar.unavailable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Set.of(start.toLocalDate().plusWeeks(1), start.toLocalDate().plusWeeks(2)));
        EventEdit input = new EventEdit(List.of(t.id()), "ACTIVITE", start, start.plusHours(1), null, "Studio", null, "", "");
        var result = creation.create(new EventCreationService.Creation(input, List.of(), 4), login(admin));
        assertThat(result.nombre()).isEqualTo(2);
        assertThat(result.evenements()).extracting(EventView::debut).containsExactly(start, start.plusWeeks(3));
        assertThat(result.evenements()).allSatisfy(e -> {
            assertThat(e.rendezVous()).isEqualTo(e.debut().minusHours(1));
            assertThat(e.convocations()).hasSize(1);
        });
    }
    @Test void sectionsExpandAcrossActivitiesAndExplicitTeamTargetsAreDeduplicated() {
        TeamView t = team("Danseurs");
        Activite other = new Activite(); other.setNom("Autre danse"); other.setSection(activity.getSection()); em.persist(other); em.flush();
        Equipe otherTeam = activityTeams.ensureFirstTeam(other);
        var result = creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(activity.getSection().getId()), 1), login(admin));
        assertThat(result.nombre()).isEqualTo(2);
        assertThat(sessions.findByActivite_IdOrderByDebutAsc(activity.getId())).hasSize(1);
        assertThat(sessions.findByActivite_IdOrderByDebutAsc(other.getId())).singleElement()
                .satisfies(e -> assertThat(e.getEquipes()).containsExactly(otherTeam));
        org.mockito.Mockito.verifyNoInteractions(calendar);
    }
    @Test void refusesUnmanagedSectionsAndInvalidRepetitionBeforeCreatingAnything() {
        TeamView t = team("Danseurs");
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(activity.getSection().getId()), 4), login(coach)))
                .hasMessageContaining("403");
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 105), login(admin)))
                .hasMessageContaining("104");
        assertThat(sessions.findAll()).isEmpty();
    }
    @Test void fullyExcludedPeriodsCreateNoEvents() {
        TeamView t = team("Danseurs");
        org.mockito.Mockito.when(calendar.unavailable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Set.of(start.toLocalDate(), start.toLocalDate().plusWeeks(1)));
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 2), login(admin)))
                .hasMessageContaining("Toutes les dates");
        assertThat(sessions.findAll()).isEmpty();
    }
    @Test void calendarFailureCreatesNoEvents() {
        TeamView t = team("Danseurs");
        org.mockito.Mockito.when(calendar.unavailable(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Calendrier indisponible"));
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 2), login(admin)))
                .hasMessageContaining("503");
        assertThat(sessions.findAll()).isEmpty();
    }

    @Test void defaultTeamReceivesAllRegistrationsWithoutAnOptionInAnySection() {
        Equipe first = activityTeams.ensureFirstTeam(activity);
        activityTeams.assignNewMembership(aliceMembership); activityTeams.assignNewMembership(bobMembership);
        activityTeams.assignNewMembership(aliceMembership); em.flush(); em.clear();
        assertThat(teams.findById(first.getId()).orElseThrow().getAffectations())
                .extracting(a -> a.getAdhesion().getId()).containsExactlyInAnyOrder(aliceMembership.getId(), bobMembership.getId());
        assertThat(service.state(activity.getId(), login(admin)).equipes()).singleElement()
                .satisfies(t -> { assertThat(t.principale()).isTrue(); assertThat(t.membres()).isEqualTo(2); });
        assertThatThrownBy(() -> service.archive(activity.getId(), first.getId(), login(admin)))
                .hasMessageContaining("première équipe");
    }

    @Test void newRegistrationGoesToFirstCreatedTeamAndFileEditsPreserveManualRedistribution() {
        TeamView first = team("Z seniors"); TeamView second = team("A juniors");
        activityTeams.assignNewMembership(aliceMembership);
        EventView existingEvent = event(first);
        assertThat(teams.findById(first.id()).orElseThrow().getAffectations()).hasSize(1);
        assertThat(teams.findById(second.id()).orElseThrow().getAffectations()).isEmpty();
        service.roster(activity.getId(), first.id(), new RosterEdit(List.of(), false), login(admin));
        service.roster(activity.getId(), second.id(), new RosterEdit(List.of(aliceMembership.getId()), false), login(admin));
        activityTeams.assignNewMembership(aliceMembership);
        Adhesion newcomer = membership(outsider, activity); activityTeams.saveMembership(newcomer); em.flush();
        assertThat(sessions.findById(existingEvent.id()).orElseThrow().getPresences()).singleElement()
                .satisfies(p -> assertThat(p.getAdhesion().getId()).isEqualTo(aliceMembership.getId()));
        assertThat(service.state(activity.getId(), login(admin)).equipes()).satisfies(list -> {
            assertThat(list.get(0).effectif()).containsExactly(newcomer.getId());
            assertThat(list.get(1).effectif()).containsExactly(aliceMembership.getId());
        });
    }

    @Test void surclassementIsAlsoAssignedToTheTargetActivityDefaultTeam() {
        Activite target = new Activite(); target.setNom("Danse confirmés"); target.setSection(activity.getSection()); em.persist(target);
        aliceMembership.setSurClassement(target); activityTeams.saveMembership(aliceMembership); em.flush();
        assertThat(teams.findByActivite_IdOrderByIdAsc(target.getId())).singleElement()
                .satisfies(t -> assertThat(t.getAffectations()).singleElement()
                        .satisfies(a -> assertThat(a.getAdhesion().getId()).isEqualTo(aliceMembership.getId())));
    }

    @Test void responsibilitiesBelongToEventsAndRequireTheEventManager() {
        TeamView t = team("A", aliceMembership.getId()); EventView event = event(t);
        TacheSeance task = new TacheSeance(); task.setNom("Matériel"); em.persist(task); em.flush();
        var input = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), t.id()));
        assertThat(service.responsibilities(event.id(), input, login(coach)).responsabilites()).singleElement()
                .satisfies(r -> assertThat(r.getAdherent().getId()).isEqualTo(alice.getId()));
        assertThat(service.responsibilityCandidates(event.id(), login(coach))).extracting(r -> r.id()).contains(alice.getId());
        assertThatThrownBy(() -> service.responsibilities(event.id(), List.of(), login(outsider))).hasMessageContaining("403");
        assertThat(service.responsibilities(event.id(), List.of(), login(coach)).responsabilites()).isEmpty();
    }

    @Test void responsibilityTeamsCoverTheSectionAndPreferTheFirstReferentEvenWithoutMembership() {
        TeamView host = team("Hôte", aliceMembership.getId()); EventView event = event(host);
        Activite other = new Activite(); other.setNom("Autre activité"); other.setSection(activity.getSection()); em.persist(other);
        Equipe target = new Equipe(); target.setActivite(other); target.setNom("Invités");
        target.getReferents().add(outsider); target.getReferents().add(coach); em.persist(target);
        TacheSeance task = new TacheSeance(); task.setNom("Accueil"); em.persist(task); em.flush();
        var candidates = service.responsibilityTeams(event.id(), login(coach));
        assertThat(candidates).extracting(r -> r.id()).contains(host.id(), target.getId());
        assertThat(candidates).filteredOn(r -> r.id().equals(target.getId())).singleElement()
                .satisfies(r -> assertThat(r.attributaire().id()).isEqualTo(outsider.getId()));
        var input = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), target.getId()));
        service.responsibilities(event.id(), input, login(coach)); em.flush(); em.clear();
        assertThat(sessions.findById(event.id()).orElseThrow().getResponsabilites()).singleElement().satisfies(r -> {
            assertThat(r.getAdherent().getId()).isEqualTo(outsider.getId());
            assertThat(r.getEquipeId()).isEqualTo(target.getId());
        });
        assertThatThrownBy(() -> service.responsibilityTeams(event.id(), login(parent))).hasMessageContaining("403");
    }

    @Test void responsibilityFallsBackToFirstActiveMemberAndRejectsInvalidTeamsAtomically() {
        TeamView host = team("Hôte", bobMembership.getId(), aliceMembership.getId()); EventView event = event(host);
        TacheSeance task = new TacheSeance(); task.setNom("Matériel"); em.persist(task); em.flush();
        var valid = new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), host.id());
        assertThat(service.responsibilities(event.id(), List.of(valid), login(coach)).responsabilites()).singleElement()
                .satisfies(r -> assertThat(r.getAdherent().getId()).isEqualTo(alice.getId()));
        TeamView empty = team("Vide");
        Equipe archived = new Equipe(); archived.setActivite(activity); archived.setNom("Archivée"); archived.setArchivee(true); em.persist(archived);
        Section elsewhere = Section.builder().nom("Autre section").type("NON_COMPETITIVE").build(); em.persist(elsewhere);
        Activite other = new Activite(); other.setNom("Autre"); other.setSection(elsewhere); em.persist(other);
        Equipe foreign = new Equipe(); foreign.setNom("Hors section"); foreign.setActivite(other); foreign.getReferents().add(alice); em.persist(foreign); em.flush();
        assertThat(service.responsibilityTeams(event.id(), login(coach))).extracting(r -> r.id())
                .contains(empty.id()).doesNotContain(archived.getId(), foreign.getId());
        for (Long invalidId : List.of(empty.id(), archived.getId(), foreign.getId(), Long.MAX_VALUE)) {
            var invalid = new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), invalidId);
            assertThatThrownBy(() -> service.responsibilities(event.id(), List.of(valid, invalid), login(coach)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            assertThat(sessions.findById(event.id()).orElseThrow().getResponsabilites()).singleElement()
                    .satisfies(r -> assertThat(r.getEquipeId()).isEqualTo(host.id()));
        }
        assertThatThrownBy(() -> service.responsibilities(event.id(), List.of(valid, valid), login(coach)))
                .hasMessageContaining("sans doublon");
    }

    @Test void removingARegistrationAlsoRemovesItsAutomaticTeamLinksAndInvitations() {
        TeamView t = team("A", aliceMembership.getId()); EventView event = event(t);
        service.publish(event.id(), login(coach));
        var invite = invitations.findBySeance_IdOrderByIdAsc(event.id()).getFirst();
        service.reply(event.id(), invite.getId(), true, login(parent)); em.flush();
        activityTeams.deleteMembership(aliceMembership.getId()); em.flush(); em.clear();
        assertThat(teams.findById(t.id()).orElseThrow().getAffectations()).isEmpty();
        assertThat(invitations.findBySeance_IdOrderByIdAsc(event.id())).isEmpty();
        assertThat(sessions.findById(event.id()).orElseThrow().getPresences()).isEmpty();
    }



    @Test void sharedSessionDeduplicatesRosterAndUsesExplicitMemberships() {
        TeamView a = team("A", aliceMembership.getId()); TeamView b = team("B", aliceMembership.getId(), bobMembership.getId());
        EventView e = service.createEvent(a.id(), edit(List.of(a.id(), b.id())), login(admin));
        em.flush(); em.clear();
        Seance stored = sessions.findById(e.id()).orElseThrow();
        assertThat(stored.getPresences()).hasSize(2); assertThat(stored.getEquipes()).hasSize(2);
        assertThat(invitations.findBySeance_IdOrderByIdAsc(e.id())).hasSize(2);
        assertThat(mails.count()).isZero();
    }

    @Test void publishingIsIdempotentGroupsSiblingEmailsAndParentAnswersSeparately() {
        TeamView t = team("Famille", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t);
        service.publish(e.id(), login(coach)); service.publish(e.id(), login(coach)); em.flush();
        assertThat(mails.count()).isEqualTo(1); assertThat(mails.findAll().getFirst().getDestinataire()).isEqualTo(login(parent));
        var invites = invitations.findBySeance_IdOrderByIdAsc(e.id());
        service.reply(e.id(), invites.get(0).getId(), true, login(parent));
        service.reply(e.id(), invites.get(1).getId(), false, login(parent)); em.flush();
        assertThat(invites).extracting(ConvocationEquipe::getReponse).containsExactly(true, false);
        assertThat(invites.getFirst().getReponduPar()).isEqualTo(parent.getUser().getId());
        assertThat(invites.getFirst().getHistorique()).hasSize(1);
        assertThat(sessions.findById(e.id()).orElseThrow().getPresences()).allSatisfy(p -> assertThat(p.getPresence()).isNull());
        service.attendance(e.id(), invites.getFirst().getId(), false, login(coach));
        assertThat(invites.getFirst().getReponse()).isTrue();
        assertThat(service.events(t.id(), login(parent)).getFirst().convocations()).hasSize(2);
    }

    @Test void familyMembershipDoesNotGrantAuthorityOverAnotherAdult() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin));
        Tribu family = new Tribu(UUID.randomUUID()); em.persist(family); outsider.setTribu(family); alice.setTribu(family); em.flush();
        Long invitationId = invitations.findBySeance_IdOrderByIdAsc(e.id()).getFirst().getId();
        assertThatThrownBy(() -> service.reply(e.id(), invitationId, true, login(outsider)))
                .hasMessageContaining("403");
        assertThatThrownBy(() -> service.state(activity.getId(), login(coach))).hasMessageContaining("403");

    }

    @Test void selectionAllowsReinforcementWithoutMovingTeamsAndRestrictsPersonalCalendar() {
        TeamView a = team("A", aliceMembership.getId()); TeamView b = team("B", bobMembership.getId()); EventView e = event(a);
        service.select(e.id(), List.of(bobMembership.getId()), login(coach)); service.publish(e.id(), login(coach)); em.flush();
        assertThat(teams.findById(a.id()).orElseThrow().getAffectations()).extracting(h -> h.getAdhesion().getId()).containsExactly(aliceMembership.getId());
        assertThat(teams.findById(b.id()).orElseThrow().getAffectations()).extracting(h -> h.getAdhesion().getId()).containsExactly(bobMembership.getId());
        var from = start.minusDays(1); var until = start.plusDays(1); var excluded = List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label);
        assertThat(sessions.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(alice.getId(), excluded, from, until)).isEmpty();
        assertThat(sessions.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(bob.getId(), excluded, from, until)).extracting(Seance::getId).containsExactly(e.id());
        assertThat(mails.count()).isEqualTo(1);
    }

    @Test void crossActivityIdsAndCanceledMembershipsAreRejected() {
        TeamView a = team("A");
        Activite other = new Activite(); other.setNom("Autre"); other.setSection(activity.getSection()); em.persist(other);
        Adhesion outside = membership(outsider, other); em.flush();
        assertThatThrownBy(() -> service.roster(activity.getId(), a.id(), new RosterEdit(List.of(outside.getId()), false), login(admin))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.saveTeam(other.getId(), a.id(), new TeamEdit("Invalid", "", List.of(), List.of()), login(admin))).hasMessageContaining("404");
        aliceMembership.setStatutActuel(Status.ANNULEE.label);
        assertThatThrownBy(() -> service.roster(activity.getId(), a.id(), new RosterEdit(List.of(aliceMembership.getId()), false), login(admin))).hasMessageContaining("400");
    }

    @Test void reschedulingResetsAnswersButPreservesHistoryAndCancelAllowsDisabling() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin));
        ConvocationEquipe c = invitations.findBySeance_IdOrderByIdAsc(e.id()).getFirst(); service.reply(e.id(), c.getId(), true, login(parent));

        start = start.plusDays(1); service.reschedule(e.id(), edit(List.of(t.id())), login(coach));
        assertThat(c.getReponse()).isNull(); assertThat(c.getHistorique()).hasSize(1);
        service.cancel(e.id(), login(coach));

        assertThat(teams.findById(t.id())).isPresent();
    }

    @Test void remindersAreIdempotentAndOnlyTargetPublishedUnansweredInvitations() {
        TeamView t = team("A", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin));
        var list = invitations.findBySeance_IdOrderByIdAsc(e.id()); service.reply(e.id(), list.getFirst().getId(), true, login(parent));
        String request = UUID.randomUUID().toString(); service.remind(e.id(), request, login(coach)); service.remind(e.id(), request, login(coach)); em.flush();
        assertThat(mails.count()).isEqualTo(2);
        assertThat(mails.findAll().stream().filter(m -> m.getSujet().startsWith("RELANCE")).findFirst().orElseThrow().getConvocations()).containsExactly(list.get(1).getId());
    }

    @Test void rosterRefreshLeavesPublishedOrPointedSessionsIntact() {
        TeamView t = team("A", aliceMembership.getId()); EventView published = event(t); service.publish(published.id(), login(admin));
        EventView draft = event(t);
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(bobMembership.getId()), true), login(admin)); em.flush(); em.clear();
        assertThat(sessions.findById(published.id()).orElseThrow().getPresences()).extracting(p -> p.getAdhesion().getId()).containsExactly(aliceMembership.getId());
        assertThat(sessions.findById(draft.id()).orElseThrow().getPresences()).extracting(p -> p.getAdhesion().getId()).containsExactly(bobMembership.getId());
        assertThat(invitations.findBySeance_IdOrderByIdAsc(draft.id())).extracting(c -> c.getAdhesion().getId()).containsExactly(bobMembership.getId());
    }

    @Test void republishingAWithdrawnParticipantProducesANewInvitationEmail() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        service.publish(e.id(), login(admin)); service.select(e.id(), List.of(), login(admin));
        service.select(e.id(), List.of(aliceMembership.getId()), login(admin)); service.publish(e.id(), login(admin)); em.flush();
        assertThat(mails.findAll().stream().filter(m -> m.getType().equals("CONVOCATION"))).hasSize(2);
    }

    @Test void anAdultWithoutARepresentativeFindsTheirOwnPublishedInvitation() {
        alice.setRepresentant(null); alice.setEmailRepresentant(false);
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin)); em.flush();
        assertThat(invitations.findPublishedForUser(login(alice))).hasSize(1);
        assertThat(invitations.findPublishedForUser(login(parent))).isEmpty();
    }

    @Test void aRemovedCoachLosesAccessToTheTeamAndItsAttendance() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        assertThat(sessions.findByIdAndManagerUsername(e.id(), login(coach))).isPresent();
        service.saveTeam(activity.getId(), t.id(), new TeamEdit(t.nom(), "", List.of(), List.of()), login(admin)); em.flush();
        assertThat(service.mine(login(coach))).isEmpty();
        assertThat(sessions.findByIdAndManagerUsername(e.id(), login(coach))).isEmpty();
        assertThatThrownBy(() -> service.publish(e.id(), login(coach))).hasMessageContaining("403");
    }

    @Test void aMeetingAfterTheEventStartIsRejectedWithAnExplanationAndNoEventIsSaved() {
        TeamView t = team("A", aliceMembership.getId());
        EventEdit invalid = new EventEdit(List.of(t.id()), "AUTRE", start, start.plusHours(1),
                null, "Studio", start.plusMinutes(23), "", "Informations");
        assertThatThrownBy(() -> service.createEvent(t.id(), invalid, login(admin)))
                .hasMessageContaining("Le rendez-vous ne peut pas être postérieur au début de l’événement.");
        assertThat(sessions.count()).isZero();
        EventEdit corrected = new EventEdit(List.of(t.id()), "AUTRE", start, start.plusHours(1),
                null, "Studio", start.minusMinutes(15), "", "Informations");
        EventView saved = service.createEvent(t.id(), corrected, login(admin));
        assertThat(saved.rendezVous()).isEqualTo(start.minusMinutes(15));
        assertThat(sessions.count()).isEqualTo(1);
    }
}
