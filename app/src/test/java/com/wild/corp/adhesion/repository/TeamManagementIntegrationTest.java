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
@Import({EventChatService.class, EventCreationService.class, TeamManagementService.class, PresenceServices.class, ActivityTeamService.class, ResponsabiliteSeanceServices.class})
class TeamManagementIntegrationTest {
    @SpringBootConfiguration @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Seance.class) @EnableJpaRepositories(basePackageClasses = EquipeRepository.class)
    static class Config { }
    @Autowired EntityManager em;
    @Autowired TeamManagementService service;
    @Autowired EventChatService chat;
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
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(memberIds)), login(admin)); return t;
    }
    EventEdit edit(List<Long> teamIds) {
        return new EventEdit(teamIds, "ACTIVITE", start, start.plusHours(1), null, "Studio", start.minusMinutes(15), "", "Venir en tenue");
    }
    EventView event(TeamView t) { return service.createEvent(t.id(), edit(List.of(t.id())), login(admin)); }

    @Test void everyoneCanReadEventDetailsWhileInvitationsAndChatKeepTheirPermissions() {
        TeamView t = team("Équipe", aliceMembership.getId(), bobMembership.getId());
        EventView e = event(t);
        EventDetails details = service.eventDetails(e.id(), login(admin));
        assertThat(details.activiteId()).isEqualTo(activity.getId());
        assertThat(details.activiteNom()).isEqualTo("Danse adultes");
        assertThat(details.sectionNom()).isEqualTo("Danse");
        assertThat(details.evenement().convocations()).hasSize(2);
        assertThat(details.canChat()).isTrue();
        EventDetails publicDetails = service.eventDetails(e.id(), login(outsider));
        assertThat(publicDetails.activiteNom()).isEqualTo(details.activiteNom());
        assertThat(publicDetails.evenement().commentaire()).isEqualTo("Venir en tenue");
        assertThat(publicDetails.evenement().canManage()).isFalse();
        assertThat(publicDetails.evenement().convocations()).isEmpty();
        assertThat(publicDetails.evenement().emails()).isEmpty();
        assertThat(publicDetails.canChat()).isFalse();
        assertThatThrownBy(() -> service.eventDetails(Long.MAX_VALUE, login(admin))).hasMessageContaining("404");
        service.publish(e.id(), login(admin));
        assertThat(service.eventDetails(e.id(), login(parent)).evenement().convocations()).hasSize(2);
        assertThat(service.eventDetails(e.id(), login(parent)).canChat()).isTrue();
        assertThat(service.eventDetails(e.id(), login(alice)).evenement().convocations()).singleElement()
                .satisfies(invite -> { assertThat(invite.adherentId()).isEqualTo(alice.getId()); assertThat(invite.canReply()).isTrue(); });
    }

    @Test void eventVisibilityDoesNotGrantManagementInvitationOrAttendanceRights() {
        TeamView t = team("Équipe", aliceMembership.getId()); EventView e = event(t);
        Long invitationId = e.convocations().getFirst().id();
        assertThat(service.events(t.id(), login(outsider))).extracting(EventView::id).containsExactly(e.id());
        assertThatThrownBy(() -> service.publish(e.id(), login(outsider))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.reschedule(e.id(), edit(List.of(t.id())), login(outsider))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.cancel(e.id(), login(outsider))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.responsibilities(e.id(), List.of(), login(outsider))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.attendance(e.id(), invitationId, true, login(outsider))).hasMessageContaining("403");
        service.publish(e.id(), login(admin));
        assertThatThrownBy(() -> service.reply(e.id(), invitationId, true, login(outsider))).hasMessageContaining("403");
        assertThat(service.eventDetails(e.id(), login(outsider)).evenement().convocations()).isEmpty();
        assertThat(service.eventDetails(e.id(), login(coach)).canChat()).isTrue();
        service.cancel(e.id(), login(admin));
        assertThat(service.eventDetails(e.id(), login(outsider)).evenement().etat()).isEqualTo("ANNULEE");
        assertThat(service.events(t.id(), login(outsider))).extracting(EventView::id).containsExactly(e.id());
        Seance past = sessions.findById(e.id()).orElseThrow();
        past.setDebut(start.minusYears(1)); past.setFin(start.minusYears(1).plusHours(1)); past.getEquipes().clear();
        assertThat(service.eventDetails(e.id(), login(outsider)).evenement().debut()).isEqualTo(past.getDebut());
    }

    @Test void eventMembersShowTheirActiveSectionTeamsEvenWhenNotTargetedByTheEvent() {
        TeamView host = team("Hôte", aliceMembership.getId(), bobMembership.getId());
        team("Loisirs", aliceMembership.getId());
        TeamView expired = team("Ancienne", aliceMembership.getId());
        teams.findById(expired.id()).orElseThrow().getAffectations().getFirst().setFin(start.toLocalDate());
        TeamView future = team("Future", aliceMembership.getId());
        teams.findById(future.id()).orElseThrow().getAffectations().getFirst().setDebut(start.toLocalDate().plusDays(1));
        TeamView archived = team("Archivée", aliceMembership.getId());
        teams.findById(archived.id()).orElseThrow().setArchivee(true);
        Section elsewhere = Section.builder().nom("Autre section").type("NON_COMPETITIVE").build(); em.persist(elsewhere);
        Activite other = new Activite(); other.setNom("Autre activité"); other.setSection(elsewhere); em.persist(other);
        Equipe foreign = new Equipe(); foreign.setNom("Hors section"); foreign.setActivite(other);
        foreign.getReferents().add(alice); em.persist(foreign);
        EventView event = event(host); em.flush(); em.clear();
        assertThat(service.eventDetails(event.id(), login(admin)).evenement().convocations()).hasSize(2).allSatisfy(invite -> {
            assertThat(invite.equipes()).containsExactlyElementsOf(invite.adherentId().equals(alice.getId())
                    ? List.of("Hôte", "Loisirs") : List.of("Hôte"));
        });
    }

    @Test void eventChatPersistsAuthorsAndSeparatesPaginatedHistoriesByEvent() {
        TeamView t = team("Équipe", aliceMembership.getId());
        EventView first = event(t), second = event(t);
        for (int i = 0; i < 53; i++) chat.send(first.id(), "Message " + i, login(coach));
        chat.send(second.id(), "Autre événement", login(admin));
        em.flush(); em.clear();
        var latest = chat.history(first.id(), null, null, login(alice));
        assertThat(latest).hasSize(50);
        assertThat(latest.getFirst().content()).isEqualTo("Message 3");
        assertThat(latest.getLast().content()).isEqualTo("Message 52");
        assertThat(latest.getLast().senderName()).isEqualTo("coach Test");
        assertThat(latest.getLast().senderUserId()).isEqualTo(coach.getUser().getId());
        var older = chat.history(first.id(), latest.getFirst().id(), null, login(alice));
        assertThat(older).extracting(EventChatService.Message::content).containsExactly("Message 0", "Message 1", "Message 2");
        assertThat(chat.history(first.id(), null, older.getLast().id(), login(alice)))
                .extracting(EventChatService.Message::id).containsExactlyElementsOf(latest.stream().map(EventChatService.Message::id).toList());
        assertThat(chat.history(second.id(), null, null, login(admin))).singleElement()
                .satisfies(m -> assertThat(m.content()).isEqualTo("Autre événement"));
    }

    @Test void eventChatChecksCurrentAccessForReadingAndWritingAndKeepsTheCancellationHistory() {
        TeamView t = team("Équipe", aliceMembership.getId()); EventView e = event(t);
        assertThatThrownBy(() -> chat.history(e.id(), null, null, login(outsider))).hasMessageContaining("403");
        assertThatThrownBy(() -> chat.send(e.id(), "Interdit", login(outsider))).hasMessageContaining("403");
        service.publish(e.id(), login(admin));
        chat.send(e.id(), "  Bonjour  ", login(parent));
        assertThat(chat.history(e.id(), null, null, login(parent))).singleElement()
                .satisfies(m -> assertThat(m.content()).isEqualTo("Bonjour"));
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of()), login(admin));
        assertThat(service.eventDetails(e.id(), login(parent)).canChat()).isFalse();
        assertThat(service.eventDetails(e.id(), login(parent)).evenement().id()).isEqualTo(e.id());
        assertThatThrownBy(() -> chat.history(e.id(), null, null, login(parent))).hasMessageContaining("403");
        assertThatThrownBy(() -> chat.send(e.id(), "Retiré", login(parent))).hasMessageContaining("403");
        service.cancel(e.id(), login(admin));
        assertThat(chat.history(e.id(), null, null, login(coach))).hasSize(1);
        chat.send(e.id(), "Report à venir", login(coach));
        assertThat(chat.history(e.id(), null, null, login(admin))).hasSize(2);
    }

    @Test void eventChatRejectsInvalidMessagesAndCursors() {
        EventView e = event(team("Équipe", aliceMembership.getId()));
        for (String content : Arrays.asList(null, "", "   ", "a".repeat(2001))) {
            assertThatThrownBy(() -> chat.send(e.id(), content, login(admin))).hasMessageContaining("400");
        }
        assertThatThrownBy(() -> chat.history(e.id(), 0L, null, login(admin))).hasMessageContaining("400");
        assertThatThrownBy(() -> chat.history(e.id(), null, -1L, login(admin))).hasMessageContaining("400");
        assertThatThrownBy(() -> chat.history(e.id(), 1L, 2L, login(admin))).hasMessageContaining("400");
        assertThat(chat.history(e.id(), null, null, login(admin))).isEmpty();
    }

    @Test void rosterCandidatesExposePaymentValidationRegistrationAndFlagFromTheirMembership() {
        aliceMembership.setValidPaiementSecretariat(true);
        aliceMembership.setFlag(true);
        bobMembership.setStatutActuel(Status.ATTENTE_SECRETARIAT.label);
        bobMembership.setValidPaiementSecretariat(false);
        em.flush(); em.clear();

        assertThat(service.state(activity.getId(), login(admin)).candidats())
                .extracting(Person::adhesionId, Person::paiementValide, Person::inscriptionValidee, Person::flag)
                .containsExactlyInAnyOrder(tuple(aliceMembership.getId(), true, true, true),
                        tuple(bobMembership.getId(), false, false, false));

        Adhesion updated = em.find(Adhesion.class, bobMembership.getId());
        updated.setStatutActuel(Status.LICENCE_GENEREE.label);
        assertThat(service.state(activity.getId(), login(admin)).candidats())
                .filteredOn(p -> p.adhesionId().equals(bobMembership.getId())).singleElement()
                .satisfies(p -> assertThat(p.inscriptionValidee()).isTrue());
        updated.setStatutActuel(null);
        assertThat(service.state(activity.getId(), login(admin)).candidats())
                .filteredOn(p -> p.adhesionId().equals(bobMembership.getId())).singleElement()
                .satisfies(p -> assertThat(p.inscriptionValidee()).isFalse());
    }

    @Test void memberDetailsUseTheChosenContactAndIncludeOnlyTheActivityMembership() {
        alice.setNaissance(LocalDate.of(2010, 5, 12)); alice.setTelephone("0600000001");
        alice.setAdresse("Adresse personnelle"); alice.setCodePostal("44000"); alice.setVille("Nantes");
        parent.setTelephone("0600000002"); parent.setAdresse("Adresse du représentant");
        parent.setCodePostal("44100"); parent.setVille("Nantes");
        alice.setTelephoneRepresentant(true); alice.setAdresseRepresentant(true);
        aliceMembership.setValidPaiementSecretariat(true); aliceMembership.setFlag(true);
        em.flush(); em.clear();
        MemberDetails details = service.memberDetails(activity.getId(), aliceMembership.getId(), login(admin));
        assertThat(details.personne().id()).isEqualTo(alice.getId());
        assertThat(details.personne().adhesionId()).isEqualTo(aliceMembership.getId());
        assertThat(details.personne().paiementValide()).isTrue();
        assertThat(details.personne().flag()).isTrue();
        assertThat(details.naissance()).isEqualTo(LocalDate.of(2010, 5, 12));
        assertThat(details.email()).isEqualTo(login(parent));
        assertThat(details.telephone()).isEqualTo("0600000002");
        assertThat(details.adresse()).isEqualTo("Adresse du représentant");
        assertThat(details.codePostal()).isEqualTo("44100");
        assertThat(details.representant().prenom()).isEqualTo("parent");

        Adherent updated = em.find(Adherent.class, alice.getId());
        updated.setEmailRepresentant(false); updated.setTelephoneRepresentant(false); updated.setAdresseRepresentant(false);
        details = service.memberDetails(activity.getId(), aliceMembership.getId(), login(admin));
        assertThat(details.email()).isEqualTo(login(alice));
        assertThat(details.telephone()).isEqualTo("0600000001");
        assertThat(details.adresse()).isEqualTo("Adresse personnelle");
    }

    @Test void memberDetailsRequireActivityManagementAndAnEligibleMembershipInThatActivity() {
        assertThatThrownBy(() -> service.memberDetails(activity.getId(), aliceMembership.getId(), login(outsider)))
                .hasMessageContaining("403");
        activity.getSection().getReferents().add(coach.getUser()); em.flush();
        assertThat(service.memberDetails(activity.getId(), aliceMembership.getId(), login(coach)).personne().id())
                .isEqualTo(alice.getId());
        Activite other = new Activite(); other.setNom("Autre activité"); em.persist(other);
        Adhesion foreign = membership(outsider, other); em.flush();
        assertThatThrownBy(() -> service.memberDetails(activity.getId(), foreign.getId(), login(admin)))
                .hasMessageContaining("404");
        aliceMembership.setStatutActuel(Status.ANNULEE.label);
        assertThatThrownBy(() -> service.memberDetails(activity.getId(), aliceMembership.getId(), login(admin)))
                .hasMessageContaining("404");
    }

    @Test void memberDetailsHandleMissingContactsWithoutReturningAnAccount() {
        Adhesion membership = membership(outsider, activity);
        outsider.setUser(null); em.flush();
        MemberDetails details = service.memberDetails(activity.getId(), membership.getId(), login(admin));
        assertThat(details.email()).isNull();
        assertThat(details.telephone()).isNull();
        assertThat(details.naissance()).isNull();
        assertThat(details.representant()).isNull();
    }

    @Test void eventCreationOptionsIncludeManagementRightsWithoutDirectTeamAssignments() {
        TeamView t = team("Danseurs", aliceMembership.getId());
        activity.getSection().getReferents().add(parent.getUser());
        em.flush(); em.clear();

        assertThat(service.mine(login(admin))).isEmpty();
        assertThat(service.mine(login(parent))).isEmpty();
        for (Adherent manager : List.of(admin, parent, coach)) {
            assertThat(creation.options(login(manager)).equipes()).extracting(TeamView::id).containsExactly(t.id());
            assertThat(creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 1), login(manager)).nombre())
                    .isEqualTo(1);
        }
        assertThat(creation.options(login(alice)).equipes()).isEmpty();
        assertThat(creation.options(login(outsider)).equipes()).isEmpty();
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 1), login(alice)))
                .hasMessageContaining("403");
    }

    @Test void eventCreationOptionsExcludeArchivedTeamsEvenForAdministrators() {
        TeamView t = team("Archivée");
        teams.findById(t.id()).orElseThrow().setArchivee(true);
        em.flush(); em.clear();

        assertThat(creation.options(login(admin)).equipes()).isEmpty();
        assertThatThrownBy(() -> creation.create(new EventCreationService.Creation(edit(List.of(t.id())), List.of(), 1), login(admin)))
                .hasMessageContaining("403");
        assertThat(sessions.count()).isZero();
    }

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
        service.roster(activity.getId(), first.id(), new RosterEdit(List.of()), login(admin));
        service.roster(activity.getId(), second.id(), new RosterEdit(List.of(aliceMembership.getId())), login(admin));
        activityTeams.assignNewMembership(aliceMembership);
        Adhesion newcomer = membership(outsider, activity); activityTeams.saveMembership(newcomer); em.flush();
        assertThat(sessions.findById(existingEvent.id()).orElseThrow().getPresences()).isEmpty();
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

    @Test void responsibilityRepliesAreIndependentFromParticipationAndFromOtherTasks() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        TacheSeance first = new TacheSeance(); first.setNom("Matériel"); em.persist(first);
        TacheSeance second = new TacheSeance(); second.setNom("Accueil"); em.persist(second); em.flush();
        var inputs = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(first.getId(), t.id()),
                new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(second.getId(), t.id()));
        EventView draft = service.responsibilities(e.id(), inputs, login(coach));
        assertThat(draft.convocations()).hasSize(1);
        assertThat(draft.responsabilites()).hasSize(2).allSatisfy(r -> {
            assertThat(r.invitation().publication()).isNull(); assertThat(r.invitation().reponse()).isNull();
        });
        Long participant = draft.convocations().getFirst().id();
        Long duty = draft.responsabilites().getFirst().invitation().id();
        assertThat(duty).isNotEqualTo(participant);
        service.publish(e.id(), login(coach));
        service.reply(e.id(), participant, false, login(parent));
        service.reply(e.id(), duty, true, login(parent));
        em.flush(); em.clear();
        EventView saved = service.eventDetails(e.id(), login(parent)).evenement();
        assertThat(saved.convocations().getFirst().reponse()).isFalse();
        assertThat(saved.responsabilites().getFirst().invitation().reponse()).isTrue();
        assertThat(saved.responsabilites().get(1).invitation().dateReponse()).isNull();
        assertThat(sessions.findById(e.id()).orElseThrow().getPresences()).hasSize(1)
                .allSatisfy(p -> assertThat(p.getPresence()).isNull());
        assertThatThrownBy(() -> service.attendance(e.id(), duty, true, login(coach))).hasMessageContaining("400");
        // Saving and refreshing a roster must keep each responsibility response.
        service.responsibilities(e.id(), inputs.reversed(), login(coach));
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of()), login(admin));
        assertThat(invitations.findById(duty).orElseThrow().getReponse()).isTrue();
        assertThat(invitations.findById(duty).orElseThrow().isSelectionnee()).isTrue();
        assertThat(service.eventDetails(e.id(), login(parent)).evenement().convocations()).isEmpty();
        assertThat(service.eventDetails(e.id(), login(parent)).evenement().responsabilites()).hasSize(2);
    }

    @Test void responsibilityInvitationsSupportPeopleWithoutMembershipAndRespectReplyPermissions() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        Equipe target = em.find(Equipe.class, t.id()); target.getReferents().add(outsider);
        TacheSeance task = new TacheSeance(); task.setNom("Accueil"); em.persist(task); em.flush();
        var inputs = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), t.id()));
        EventView draft = service.responsibilities(e.id(), inputs, login(coach));
        Long duty = draft.responsabilites().getFirst().invitation().id();
        assertThat(draft.responsabilites().getFirst().invitation().adhesionId()).isNull();
        // A responsibility assignee cannot reply before publication.
        target.getReferents().clear();
        assertThatThrownBy(() -> service.reply(e.id(), duty, true, login(outsider))).hasMessageContaining("403");
        assertThat(sessions.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(outsider.getId(),
                List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label), start.minusDays(1), start.plusDays(1))).isEmpty();
        service.publishInvitation(e.id(), duty, login(coach));
        assertThat(sessions.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(outsider.getId(),
                List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label), start.minusDays(1), start.plusDays(1)))
                .extracting(Seance::getId).containsExactly(e.id());
        EventView published = service.eventDetails(e.id(), login(outsider)).evenement();
        assertThat(published.convocations()).isEmpty();
        assertThat(published.responsabilites().getFirst().invitation().canReply()).isTrue();
        assertThat(service.eventDetails(e.id(), login(alice)).evenement().responsabilites().getFirst().invitation()).isNull();
        assertThatThrownBy(() -> service.reply(e.id(), duty, true, login(alice))).hasMessageContaining("403");
        service.reply(e.id(), duty, null, login(outsider));
        service.remindInvitation(e.id(), duty, UUID.randomUUID().toString(), login(coach));
        assertThat(mails.findAll()).noneMatch(m -> m.getType().equals("RELANCE"));
        assertThat(invitations.findById(duty).orElseThrow().getHistorique()).hasSize(1);
        start = start.plusDays(1); service.reschedule(e.id(), edit(List.of(t.id())), login(coach));
        assertThat(invitations.findById(duty).orElseThrow().hasReplied()).isFalse();
        assertThat(invitations.findById(duty).orElseThrow().getHistorique()).hasSize(1);
        service.remindInvitation(e.id(), duty, UUID.randomUUID().toString(), login(coach));
        assertThat(mails.findAll()).anyMatch(m -> m.getType().equals("RELANCE") && m.getConvocations().contains(duty));
        service.cancel(e.id(), login(coach));
        assertThat(service.eventDetails(e.id(), login(outsider)).evenement().responsabilites().getFirst().invitation().canReply()).isFalse();
        assertThatThrownBy(() -> service.reply(e.id(), duty, true, login(outsider))).hasMessageContaining("409");
    }

    @Test void removingAndReaddingAResponsibilityRequiresANewInvitationAndPreservesHistory() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        TacheSeance task = new TacheSeance(); task.setNom("Matériel"); em.persist(task); em.flush();
        var inputs = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), t.id()));
        Long duty = service.responsibilities(e.id(), inputs, login(coach)).responsabilites().getFirst().invitation().id();
        service.publishInvitation(e.id(), duty, login(coach));
        service.reply(e.id(), duty, true, login(parent));
        service.responsibilities(e.id(), List.of(), login(coach));
        assertThat(invitations.findById(duty).orElseThrow().isSelectionnee()).isFalse();
        assertThatThrownBy(() -> service.reply(e.id(), duty, true, login(parent))).hasMessageContaining("403");
        assertThat(mails.findAll()).anyMatch(m -> m.getType().equals("RETRAIT") && m.getConvocations().contains(duty));
        var restored = service.responsibilities(e.id(), inputs, login(coach)).responsabilites().getFirst().invitation();
        assertThat(restored.id()).isEqualTo(duty); assertThat(restored.publication()).isNull();
        assertThat(restored.reponse()).isNull(); assertThat(restored.dateReponse()).isNull();
        assertThat(invitations.findById(duty).orElseThrow().getHistorique()).hasSize(1);
        assertThatThrownBy(() -> service.reply(e.id(), duty, true, login(parent))).hasMessageContaining("403");
    }

    @Test void responsibilitiesBelongToEventsAndRequireTheEventManager() {
        TeamView t = team("A", aliceMembership.getId()); EventView event = event(t);
        TacheSeance task = new TacheSeance(); task.setNom("Matériel"); em.persist(task); em.flush();
        var input = List.of(new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), t.id()));
        assertThat(service.responsibilities(event.id(), input, login(coach)).responsabilites()).singleElement()
                .satisfies(r -> assertThat(r.adherent().id()).isEqualTo(alice.getId()));
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
        assertThat(service.eventDetails(event.id(), login(admin)).evenement().responsabilites()).singleElement().satisfies(r -> {
            assertThat(r.equipes()).containsExactly("Invités");
            assertThat(r.invitation().equipes()).containsExactly("Invités");
        });
        assertThat(service.eventDetails(event.id(), login(parent)).evenement().responsabilites()).singleElement().satisfies(r -> {
            assertThat(r.invitation()).isNull();
            assertThat(r.equipes()).containsExactly("Invités");
        });
        assertThatThrownBy(() -> service.responsibilityTeams(event.id(), login(parent))).hasMessageContaining("403");
    }

    @Test void responsibilityFallsBackToFirstActiveMemberAndRejectsInvalidTeamsAtomically() {
        TeamView host = team("Hôte", bobMembership.getId(), aliceMembership.getId()); EventView event = event(host);
        TacheSeance task = new TacheSeance(); task.setNom("Matériel"); em.persist(task); em.flush();
        var valid = new com.wild.corp.adhesion.models.resources.ResponsabiliteEquipeRequest(task.getId(), host.id());
        assertThat(service.responsibilities(event.id(), List.of(valid), login(coach)).responsabilites()).singleElement()
                .satisfies(r -> assertThat(r.adherent().id()).isEqualTo(alice.getId()));
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

    @Test void eventManagersCanReplyForParticipantsAndKeepAttendanceSeparate() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        service.publish(e.id(), login(coach));
        Long id = e.convocations().getFirst().id();
        service.attendance(e.id(), id, false, login(coach));
        activity.getSection().getReferents().add(outsider.getUser()); em.flush();
        List<Adherent> managers = List.of(coach, admin, outsider);
        Boolean[] answers = { true, false, null };
        for (int i = 0; i < managers.size(); i++) {
            Adherent manager = managers.get(i);
            assertThat(service.events(t.id(), login(manager)).getFirst().convocations().getFirst().canReply()).isTrue();
            InviteView answer = service.reply(e.id(), id, answers[i], login(manager)).convocations().getFirst();
            assertThat(answer.reponse()).isEqualTo(answers[i]);
            assertThat(answer.dateReponse()).isNotNull();
            assertThat(answer.presence()).isFalse();
        }
        em.flush(); em.clear();
        ConvocationEquipe stored = invitations.findById(id).orElseThrow();
        assertThat(stored.getReponduPar()).isEqualTo(outsider.getUser().getId());
        assertThat(stored.getHistorique()).extracting(ReponseConvocation::getReponse).containsExactly(true, false, null);
    }

    @Test void onlyManagersCanReplyBeforePublicationOrAfterTheEventStarts() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        Long id = e.convocations().getFirst().id();
        assertThat(e.convocations().getFirst().canReply()).isTrue();
        assertThatThrownBy(() -> service.reply(e.id(), id, true, login(parent))).hasMessageContaining("403");
        InviteView draft = service.reply(e.id(), id, true, login(coach)).convocations().getFirst();
        assertThat(draft.publication()).isNull(); assertThat(draft.presence()).isNull();
        assertThat(mails.count()).isZero();
        service.publish(e.id(), login(coach));
        Seance session = sessions.findById(e.id()).orElseThrow();
        session.setDebut(LocalDateTime.now().minusHours(2)); session.setFin(LocalDateTime.now().minusHours(1));
        session.setEtatSeance(ESeance.REALISEE); em.flush();
        assertThat(service.events(t.id(), login(parent)).getFirst().convocations().getFirst().canReply()).isFalse();
        assertThatThrownBy(() -> service.reply(e.id(), id, false, login(parent))).hasMessageContaining("409");
        InviteView past = service.reply(e.id(), id, false, login(coach)).convocations().getFirst();
        assertThat(past.canReply()).isTrue(); assertThat(past.reponse()).isFalse(); assertThat(past.presence()).isNull();
    }

    @Test void managerRepliesRejectCancelledEventsRemovedParticipantsAndInvitationsFromOtherEvents() {
        TeamView t = team("A", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t);
        Long id = e.convocations().getFirst().id();
        EventView other = event(t);
        assertThatThrownBy(() -> service.reply(other.id(), id, true, login(coach))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.reply(e.id(), id, true, login(outsider))).hasMessageContaining("403");
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(bobMembership.getId())), login(admin));
        assertThat(service.events(t.id(), login(coach)).stream().filter(s -> s.id().equals(e.id()))
                .flatMap(s -> s.convocations().stream()).filter(c -> c.id().equals(id)))
                .singleElement().satisfies(c -> assertThat(c.canReply()).isFalse());
        assertThatThrownBy(() -> service.reply(e.id(), id, true, login(coach))).hasMessageContaining("403");
        service.cancel(other.id(), login(coach));
        assertThat(service.events(t.id(), login(coach)).stream().filter(s -> s.id().equals(other.id())))
                .singleElement().satisfies(s -> assertThat(s.convocations()).allSatisfy(c -> assertThat(c.canReply()).isFalse()));
        assertThatThrownBy(() -> service.reply(other.id(), other.convocations().getFirst().id(), true, login(coach)))
                .hasMessageContaining("409");
    }

    @Test void undecidedAnswersPersistSeparatelyFromUnansweredInvitationsAndActualAttendance() {
        TeamView t = team("A", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t);
        service.publish(e.id(), login(admin));
        assertThat(service.events(t.id(), login(parent)).getFirst().convocations()).allSatisfy(c -> {
            assertThat(c.reponse()).isNull(); assertThat(c.dateReponse()).isNull(); assertThat(c.presence()).isNull();
        });
        Long id = invitations.findBySeance_IdOrderByIdAsc(e.id()).getFirst().getId();
        service.reply(e.id(), id, null, login(parent)); em.flush(); em.clear();
        ConvocationEquipe stored = invitations.findById(id).orElseThrow();
        assertThat(stored.hasReplied()).isTrue(); assertThat(stored.getReponse()).isNull();
        assertThat(stored.getDateReponse()).isNotNull(); assertThat(stored.getReponduPar()).isEqualTo(parent.getUser().getId());
        assertThat(stored.getHistorique()).singleElement().satisfies(h -> {
            assertThat(h.getReponse()).isNull(); assertThat(h.getDateReponse()).isNotNull();
        });
        var answer = service.events(t.id(), login(parent)).getFirst().convocations().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
        assertThat(answer.reponse()).isNull(); assertThat(answer.dateReponse()).isNotNull(); assertThat(answer.presence()).isNull();
        service.remind(e.id(), UUID.randomUUID().toString(), login(coach)); em.flush();
        assertThat(mails.findAll().stream().filter(m -> m.getType().equals("RELANCE"))).singleElement()
                .satisfies(m -> assertThat(m.getConvocations()).doesNotContain(id).hasSize(1));
        service.attendance(e.id(), id, true, login(coach));
        assertThat(stored.getReponse()).isNull(); assertThat(stored.getDateReponse()).isEqualTo(answer.dateReponse());
        service.reply(e.id(), id, false, login(parent)); em.flush(); em.clear();
        var updated = service.events(t.id(), login(parent)).getFirst().convocations().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
        assertThat(updated.reponse()).isFalse(); assertThat(updated.presence()).isTrue();
        assertThat(invitations.findById(id).orElseThrow().getHistorique()).hasSize(2);
        start = start.plusDays(1); service.reschedule(e.id(), edit(List.of(t.id())), login(coach)); em.flush(); em.clear();
        assertThat(invitations.findById(id).orElseThrow().hasReplied()).isFalse();
        assertThat(invitations.findById(id).orElseThrow().getHistorique()).hasSize(2);
    }

    @Test void familyMembershipDoesNotGrantAuthorityOverAnotherAdult() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin));
        Tribu family = new Tribu(UUID.randomUUID()); em.persist(family); outsider.setTribu(family); alice.setTribu(family); em.flush();
        Long invitationId = invitations.findBySeance_IdOrderByIdAsc(e.id()).getFirst().getId();
        assertThatThrownBy(() -> service.reply(e.id(), invitationId, true, login(outsider)))
                .hasMessageContaining("403");
        assertThatThrownBy(() -> service.state(activity.getId(), login(coach))).hasMessageContaining("403");

    }

    @Test void rosterChangesUpdateEventParticipantsAndRestrictPersonalCalendar() {
        TeamView a = team("A", aliceMembership.getId()); TeamView b = team("B", bobMembership.getId()); EventView e = event(a);
        TeamView updated = service.roster(activity.getId(), a.id(), new RosterEdit(List.of(bobMembership.getId())), login(admin));
        service.publish(e.id(), login(coach)); em.flush();
        assertThat(updated.effectif()).containsExactly(bobMembership.getId());
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
        assertThatThrownBy(() -> service.roster(activity.getId(), a.id(), new RosterEdit(List.of(outside.getId())), login(admin))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.saveTeam(other.getId(), a.id(), new TeamEdit("Invalid", "", List.of(), List.of()), login(admin))).hasMessageContaining("404");
        aliceMembership.setStatutActuel(Status.ANNULEE.label);
        assertThatThrownBy(() -> service.roster(activity.getId(), a.id(), new RosterEdit(List.of(aliceMembership.getId())), login(admin))).hasMessageContaining("400");
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

    @Test void individualPublicationAndRemindersOnlyTargetTheChosenSiblingAndAreIdempotent() {
        TeamView t = team("Famille", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t);
        var list = invitations.findBySeance_IdOrderByIdAsc(e.id()); Long id = list.getFirst().getId();
        service.publishInvitation(e.id(), id, login(coach)); service.publishInvitation(e.id(), id, login(coach)); em.flush();
        assertThat(list.getFirst().getPublication()).isNotNull(); assertThat(list.get(1).getPublication()).isNull();
        assertThat(mails.findAll()).singleElement().satisfies(m -> {
            assertThat(m.getDestinataire()).isEqualTo(login(parent));
            assertThat(m.getConvocations()).containsExactly(id);
        });
        String key = UUID.randomUUID().toString();
        service.remindInvitation(e.id(), id, key, login(coach));
        service.remindInvitation(e.id(), id, key, login(coach));
        service.remindInvitation(e.id(), list.get(1).getId(), UUID.randomUUID().toString(), login(coach)); em.flush();
        assertThat(mails.count()).isEqualTo(2);
        assertThat(mails.findAll()).filteredOn(m -> m.getType().equals("RELANCE")).singleElement()
                .satisfies(m -> assertThat(m.getConvocations()).containsExactly(id));
        service.reply(e.id(), id, null, login(parent));
        service.remindInvitation(e.id(), id, UUID.randomUUID().toString(), login(coach)); em.flush();
        assertThat(mails.count()).isEqualTo(2);
        service.publish(e.id(), login(coach)); em.flush();
        assertThat(mails.count()).isEqualTo(3);
        assertThat(list.get(1).getPublication()).isNotNull();
    }

    @Test void individualCommandsRequireManagementAndAnInvitationSelectedInThatEvent() {
        TeamView t = team("A", aliceMembership.getId(), bobMembership.getId()); EventView e = event(t);
        Long id = e.convocations().getFirst().id(); EventView other = event(t);
        assertThatThrownBy(() -> service.publishInvitation(e.id(), id, login(parent))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.remindInvitation(e.id(), id, UUID.randomUUID().toString(), login(parent))).hasMessageContaining("403");
        assertThatThrownBy(() -> service.publishInvitation(other.id(), id, login(coach))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.remindInvitation(other.id(), id, UUID.randomUUID().toString(), login(coach))).hasMessageContaining("400");
        invitations.findById(id).orElseThrow().setSelectionnee(false);
        assertThatThrownBy(() -> service.publishInvitation(e.id(), id, login(coach))).hasMessageContaining("400");
        assertThatThrownBy(() -> service.remindInvitation(e.id(), id, UUID.randomUUID().toString(), login(coach))).hasMessageContaining("400");
        assertThat(mails.count()).isZero();
    }

    @Test void rosterRefreshUpdatesPublishedAnsweredAndPointedEventsWithoutLosingHistory() {
        Adhesion newcomer = membership(outsider, activity);
        TeamView t = team("A", aliceMembership.getId(), bobMembership.getId());
        EventView published = event(t); service.publish(published.id(), login(admin));
        var original = invitations.findBySeance_IdOrderByIdAsc(published.id());
        ConvocationEquipe aliceInvite = original.stream().filter(c -> c.getAdherent().equals(alice)).findFirst().orElseThrow();
        ConvocationEquipe bobInvite = original.stream().filter(c -> c.getAdherent().equals(bob)).findFirst().orElseThrow();
        service.reply(published.id(), aliceInvite.getId(), null, login(parent));
        service.reply(published.id(), bobInvite.getId(), true, login(parent));
        service.attendance(published.id(), aliceInvite.getId(), true, login(coach));
        service.attendance(published.id(), bobInvite.getId(), false, login(coach));
        Long bobId = bobInvite.getId(); LocalDateTime bobPublication = bobInvite.getPublication(), bobReply = bobInvite.getDateReponse();
        EventView draft = event(t);

        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(bobMembership.getId(), newcomer.getId())), login(admin));
        em.flush(); em.clear();
        assertThat(invitations.findBySeance_IdOrderByIdAsc(published.id())).filteredOn(ConvocationEquipe::isSelectionnee)
                .extracting(c -> c.getAdhesion().getId()).containsExactlyInAnyOrder(bobMembership.getId(), newcomer.getId());
        ConvocationEquipe kept = invitations.findById(bobId).orElseThrow();
        assertThat(kept.getPublication()).isEqualTo(bobPublication); assertThat(kept.getDateReponse()).isEqualTo(bobReply);
        assertThat(kept.getReponse()).isTrue(); assertThat(kept.getHistorique()).hasSize(1);
        assertThat(sessions.findById(published.id()).orElseThrow().getPresences()).filteredOn(p -> p.getAdhesion().getId().equals(bobMembership.getId()))
                .singleElement().satisfies(p -> assertThat(p.getPresence()).isFalse());
        ConvocationEquipe removed = invitations.findById(aliceInvite.getId()).orElseThrow();
        assertThat(removed.isSelectionnee()).isFalse(); assertThat(removed.hasReplied()).isTrue();
        assertThat(removed.getHistorique()).hasSize(1);
        assertThat(sessions.findById(published.id()).orElseThrow().getPresences()).filteredOn(p -> p.getAdhesion().getId().equals(aliceMembership.getId()))
                .singleElement().satisfies(p -> assertThat(p.getPresence()).isTrue());
        assertThat(invitations.findBySeance_IdOrderByIdAsc(published.id())).filteredOn(c -> c.getAdhesion().getId().equals(newcomer.getId()))
                .singleElement().satisfies(c -> { assertThat(c.isSelectionnee()).isTrue(); assertThat(c.getPublication()).isNull(); });
        assertThat(invitations.findBySeance_IdOrderByIdAsc(draft.id())).filteredOn(ConvocationEquipe::isSelectionnee)
                .extracting(c -> c.getAdhesion().getId()).containsExactlyInAnyOrder(bobMembership.getId(), newcomer.getId());
        assertThat(sessions.findById(draft.id()).orElseThrow().getPresences()).extracting(p -> p.getAdhesion().getId())
                .containsExactlyInAnyOrder(bobMembership.getId(), newcomer.getId());
        assertThat(mails.findAll()).filteredOn(m -> m.getType().equals("RETRAIT")).hasSize(1);
        assertThat(sessions.findAllByAdherentAndStatutNonExcluAndDebutBetweenOrderByDebut(alice.getId(),
                List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label), start.minusDays(1), start.plusDays(1))).isEmpty();
    }

    @Test void rosterRefreshDeduplicatesSharedTeamsAndSupportsSuccessiveRemovalAndAddition() {
        TeamView a = team("A", aliceMembership.getId()); TeamView b = team("B", aliceMembership.getId(), bobMembership.getId());
        EventView shared = service.createEvent(a.id(), edit(List.of(a.id(), b.id())), login(admin));
        service.roster(activity.getId(), a.id(), new RosterEdit(List.of()), login(admin));
        assertThat(invitations.findBySeance_IdOrderByIdAsc(shared.id())).filteredOn(ConvocationEquipe::isSelectionnee).hasSize(2);
        service.roster(activity.getId(), b.id(), new RosterEdit(List.of(bobMembership.getId())), login(admin));
        service.roster(activity.getId(), a.id(), new RosterEdit(List.of(aliceMembership.getId())), login(admin));
        em.flush(); em.clear();
        assertThat(sessions.findById(shared.id()).orElseThrow().getPresences()).extracting(p -> p.getAdhesion().getId())
                .containsExactlyInAnyOrder(aliceMembership.getId(), bobMembership.getId());
        assertThat(invitations.findBySeance_IdOrderByIdAsc(shared.id())).filteredOn(ConvocationEquipe::isSelectionnee).hasSize(2);
    }

    @Test void rosterRefreshPreservesPastCancelledAndUnrelatedEvents() {
        TeamView a = team("A", aliceMembership.getId()); TeamView b = team("B", bobMembership.getId());
        EventView past = event(a); sessions.findById(past.id()).orElseThrow().setDebut(LocalDateTime.now().minusDays(1));
        EventView cancelled = event(a); service.cancel(cancelled.id(), login(admin));
        EventView unrelated = event(b);
        service.roster(activity.getId(), a.id(), new RosterEdit(List.of(aliceMembership.getId(), bobMembership.getId())), login(admin));
        em.flush(); em.clear();
        for (Long id : List.of(past.id(), cancelled.id())) {
            assertThat(invitations.findBySeance_IdOrderByIdAsc(id)).filteredOn(ConvocationEquipe::isSelectionnee)
                    .extracting(c -> c.getAdhesion().getId()).containsExactly(aliceMembership.getId());
        }
        assertThat(invitations.findBySeance_IdOrderByIdAsc(unrelated.id())).filteredOn(ConvocationEquipe::isSelectionnee)
                .extracting(c -> c.getAdhesion().getId()).containsExactly(bobMembership.getId());
    }

    @Test void republishingAWithdrawnParticipantProducesANewInvitationEmail() {
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t);
        service.publish(e.id(), login(admin));
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of()), login(admin));
        service.roster(activity.getId(), t.id(), new RosterEdit(List.of(aliceMembership.getId())), login(admin));
        service.publish(e.id(), login(admin)); em.flush();
        assertThat(mails.findAll().stream().filter(m -> m.getType().equals("CONVOCATION"))).hasSize(2);
    }

    @Test void anAdultWithoutARepresentativeFindsTheirOwnPublishedInvitation() {
        alice.setRepresentant(null); alice.setEmailRepresentant(false);
        TeamView t = team("A", aliceMembership.getId()); EventView e = event(t); service.publish(e.id(), login(admin)); em.flush();
        assertThat(invitations.findPublishedForUser(login(alice))).hasSize(1);
        assertThat(invitations.findPublishedForUser(login(parent))).isEmpty();
    }

    @Test void myTeamsOnlyIncludesDirectCoachAndReferentAssignmentsRegardlessOfAccountRole() {
        TeamView member = team("Membre", aliceMembership.getId());
        TeamView coached = team("Encadrée");
        TeamView referred = team("Référente");
        teams.findById(coached.id()).orElseThrow().getEncadrants().add(alice);
        teams.findById(referred.id()).orElseThrow().getReferents().add(alice);
        // Being both referent and coach still produces just one button.
        teams.findById(referred.id()).orElseThrow().getEncadrants().add(alice);
        em.flush(); em.clear();

        assertThat(service.mine(login(alice))).extracting(TeamView::id)
                .containsExactly(coached.id(), referred.id()).doesNotContain(member.id());
        assertThat(service.mine(login(outsider))).isEmpty();
    }

    @Test void myTeamsDoesNotIncludeMembershipsPublishedInvitationsOrAssignmentsOfRepresentedPeople() {
        TeamView member = team("Membre", aliceMembership.getId(), bobMembership.getId());
        teams.findById(member.id()).orElseThrow().getReferents().add(bob);
        EventView e = event(member);
        service.publish(e.id(), login(admin));
        em.flush(); em.clear();

        assertThat(invitations.findPublishedForUser(login(alice))).isNotEmpty();
        assertThat(invitations.findPublishedForUser(login(parent))).isNotEmpty();
        assertThat(service.mine(login(alice))).isEmpty();
        assertThat(service.mine(login(parent))).isEmpty();
        assertThat(service.mine(login(bob))).extracting(TeamView::id).containsExactly(member.id());
        assertThat(service.events(member.id(), login(parent))).extracting(EventView::id).containsExactly(e.id());
    }

    @Test void myTeamsRequiresAnAssignmentEvenForAnAdministratorOrSectionManager() {
        TeamView t = team("Équipe");
        activity.getSection().getReferents().add(parent.getUser());
        em.flush();

        assertThat(service.mine(login(admin))).isEmpty();
        assertThat(service.mine(login(parent))).isEmpty();
        teams.findById(t.id()).orElseThrow().getReferents().add(admin);
        teams.findById(t.id()).orElseThrow().getEncadrants().add(parent);
        em.flush(); em.clear();
        assertThat(service.mine(login(admin))).extracting(TeamView::id).containsExactly(t.id());
        assertThat(service.mine(login(parent))).extracting(TeamView::id).containsExactly(t.id());
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
