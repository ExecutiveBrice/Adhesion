package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.ESeance;

import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.Equipe;
import com.wild.corp.adhesion.models.resources.ResponsabiliteSeanceRequest;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.repository.AdherentRepository;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiviteServicesTest {

    @org.junit.jupiter.api.BeforeEach
    void activityLockUsesTheSameFixtureAsNormalReads() {
        org.mockito.Mockito.lenient().when(activiteRepository.findForTeamUpdate(any()))
                .thenAnswer(call -> activiteRepository.findById(call.getArgument(0)));
    }

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private com.wild.corp.adhesion.repository.UserRepository userRepository;

    @Mock
    private com.wild.corp.adhesion.repository.EquipeRepository equipeRepository;

    @Mock
    private AdherentRepository adherentRepository;

    @Mock
    private ActivityTeamService activityTeams;

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private ResponsabiliteSeanceServices responsabiliteSeanceServices;

    @Mock
    private AdherentServices adherentServices;

    @Test
    void resolvesSectionFromDatabaseWhenSavingActivity() {
        Activite activite = new Activite();
        activite.setSection(Section.builder().id(7L).nom("Nom envoyé").type("COMPETITION").build());
        Section section = Section.builder().id(7L).nom("Yoga").type("NON_COMPETITIVE").build();
        when(sectionRepository.findById(7L)).thenReturn(Optional.of(section));
        when(activiteRepository.saveAndFlush(activite)).thenReturn(activite);

        Activite resultat = activiteServices.save(activite);
        verify(activityTeams).ensureFirstTeam(resultat);

        assertThat(resultat.getSection()).isSameAs(section);
        assertThat(resultat.getGroupeFiltre()).isEqualTo("Yoga");
        assertThat(resultat.getGroupe()).isEqualTo("NON_COMPETITIVE");
        section.setNom("Yoga adultes");
        assertThat(resultat.getGroupeFiltre()).isEqualTo("Yoga adultes");
    }

    @Test
    void rejectsUnknownSection() {
        Activite activite = new Activite();
        activite.setSection(Section.builder().id(99L).build());
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activiteServices.save(activite))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Section introuvable");
        verify(activiteRepository, never()).save(any());
    }

    @Test
    void managedSaveAllowsOnlyActivitiesInAnAssignedSection() {
        Section managed = Section.builder().id(7L).nom("Yoga").type("NON_COMPETITIVE").build();
        Section other = Section.builder().id(8L).nom("Danse").build();
        when(sectionRepository.findManagedByUsername("referent@example.org")).thenReturn(List.of(managed));

        Activite newOutside = new Activite();
        newOutside.setSection(other);
        assertThatThrownBy(() -> activiteServices.saveManaged("referent@example.org", newOutside))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");

        Activite existingOutside = new Activite();
        existingOutside.setId(21L);
        existingOutside.setSection(other);
        Activite movedIntoManagedSection = new Activite();
        movedIntoManagedSection.setId(21L);
        movedIntoManagedSection.setSection(managed);
        when(activiteRepository.findById(21L)).thenReturn(Optional.of(existingOutside));
        assertThatThrownBy(() -> activiteServices.saveManaged("referent@example.org", movedIntoManagedSection))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");

        Activite newManaged = new Activite();
        newManaged.setSection(Section.builder().id(7L).build());
        when(sectionRepository.findById(7L)).thenReturn(Optional.of(managed));
        when(activiteRepository.saveAndFlush(newManaged)).thenReturn(newManaged);
        assertThat(activiteServices.saveManaged("referent@example.org", newManaged).getSection()).isSameAs(managed);
        verify(activiteRepository).saveAndFlush(newManaged);
    }

    @Test
    void managedPageUsesScopedSpecification() {
        Section managed = Section.builder().id(7L).nom("Yoga").build();
        when(sectionRepository.findManagedByUsername("referent@example.org")).thenReturn(List.of(managed));
        when(activiteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        activiteServices.getManagedPage("referent@example.org", "", null, null, null, null, "",
                PageRequest.of(0, 20));

        verify(activiteRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(activiteRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void detailReturnsAnActivityWithCountersAndRejectsMissingIds() {
        Activite activity = new Activite();
        activity.setId(21L);
        activity.getAdhesions().add(adhesionAvecStatut(Status.VALIDEE));
        when(activiteRepository.findById(21L)).thenReturn(Optional.of(activity));

        assertThat(activiteServices.getDetail(21L)).isSameAs(activity);
        assertThat(activity.getNbAdhesionsCompletes()).isEqualTo(1);
        assertThatThrownBy(() -> activiteServices.getDetail(99L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void managedDetailOnlyReturnsActivitiesInAssignedSections() {
        Section managed = Section.builder().id(7L).nom("Yoga").build();
        when(sectionRepository.findManagedByUsername("referent@example.org")).thenReturn(List.of(managed));
        Activite activity = new Activite();
        activity.setId(21L);
        activity.setSection(managed);
        when(activiteRepository.findById(21L)).thenReturn(Optional.of(activity));

        assertThat(activiteServices.getManagedDetail("referent@example.org", 21L)).isSameAs(activity);
        activity.setSection(Section.builder().id(8L).build());
        assertThatThrownBy(() -> activiteServices.getManagedDetail("referent@example.org", 21L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");
        assertThatThrownBy(() -> activiteServices.getManagedDetail("membre@example.org", 21L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void managedPageRejectsAnAccountWithoutAssignedSections() {
        when(sectionRepository.findManagedByUsername("membre@example.org")).thenReturn(List.of());

        assertThatThrownBy(() -> activiteServices.getManagedPage("membre@example.org", "", null, null,
                null, null, "", PageRequest.of(0, 20)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");
        org.mockito.Mockito.verifyNoInteractions(activiteRepository);
    }

    @Test
    void coachWithoutManagedSectionsCanReadAnEmptyScopedPageButCannotSaveActivities() {
        var coach = new com.wild.corp.adhesion.models.User("coach@example.org", "password");
        coach.getRoles().add(ERole.ROLE_ENCADRANT);
        when(userRepository.findByUsername(coach.getUsername())).thenReturn(Optional.of(coach));
        when(activiteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        assertThat(activiteServices.getManagedPage(coach.getUsername(), "Danse", null, null,
                null, null, "", PageRequest.of(0, 20)).getTotalElements()).isZero();
        verify(activiteRepository, never()).findAll(any(Pageable.class));
        assertThatThrownBy(() -> activiteServices.saveManaged(coach.getUsername(), new Activite()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void managedEncadrantsExposeOnlyNamesAndRequireAnAssignedSection() {
        Adherent encadrant = new Adherent();
        encadrant.setId(9L);
        encadrant.setPrenom("Alice");
        encadrant.setNom("Martin");
        when(sectionRepository.findManagedByUsername("referent@example.org"))
                .thenReturn(List.of(Section.builder().id(7L).build()));
        when(adherentRepository.findByUserRole(ERole.ROLE_ENCADRANT)).thenReturn(List.of(encadrant));

        assertThat(activiteServices.getManagedEncadrants("referent@example.org"))
                .containsExactly(new ActiviteServices.EncadrantOption(9L, "Alice", "Martin"));
        assertThatThrownBy(() -> activiteServices.getManagedEncadrants("autre@example.org"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void requiresASectionBeforeCreatingOrUpdatingAnActivity() {
        for (Long id : new Long[]{null, 12L}) {
            for (Section section : new Section[]{null, new Section(), Section.builder().id(0L).build()}) {
                Activite activite = new Activite();
                activite.setId(id);
                activite.setSection(section);
                assertThatThrownBy(() -> activiteServices.save(activite))
                        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                        .hasMessageContaining("Sélectionnez une section existante")
                        .satisfies(exception -> assertThat(((org.springframework.web.server.ResponseStatusException) exception)
                                .getStatusCode().value()).isEqualTo(400));
            }
        }
        org.mockito.Mockito.verifyNoInteractions(activiteRepository, sectionRepository, activityTeams);
    }

    @InjectMocks
    private ActiviteServices activiteServices;

    @Test
    void returnsPagedActivitiesWithCountersAndRequiredSort() {
        Activite activite = new Activite();
        activite.setNom("Yoga");
        activite.getAdhesions().add(adhesionAvecStatut(Status.VALIDEE));
        activite.getAdhesions().add(adhesionAvecStatut(Status.ATTENTE_ADHERENT));
        activite.getAdhesions().add(adhesionAvecStatut(Status.LISTE_ATTENTE));
        activite.getSeances().add(seanceAvecEtat(ESeance.REALISEE));
        activite.getSeances().add(seanceAvecEtat(ESeance.PROGRAMMEE));
        activite.getSeances().add(seanceAvecEtat(ESeance.ANNULEE));

        when(activiteRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activite), PageRequest.of(1, 10), 21));

        Page<Activite> resultat = activiteServices.getPage(" ", null, null, null, null, "",
                PageRequest.of(1, 10, Sort.by("tarif")));

        assertThat(resultat.getContent()).containsExactly(activite);
        assertThat(activite.getNbAdhesionsCompletes()).isEqualTo(1);
        assertThat(activite.getNbAdhesionsEnCours()).isEqualTo(1);
        assertThat(activite.getNbAdhesionsAttente()).isEqualTo(1);
        assertThat(activite.getNbEvenementsRealises()).isEqualTo(1);
        assertThat(activite.getNbEvenementsTotal()).isEqualTo(2);

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(activiteRepository).findAll(pageableCaptor.capture());
        Pageable pageableUtilise = pageableCaptor.getValue();
        assertThat(pageableUtilise.getPageNumber()).isEqualTo(1);
        assertThat(pageableUtilise.getPageSize()).isEqualTo(10);
        assertThat(pageableUtilise.getSort().stream().map(Sort.Order::getProperty))
                .containsExactly("nom", "id");
        assertThat(pageableUtilise.getSort().stream().map(Sort.Order::getDirection))
                .containsOnly(Sort.Direction.ASC);
    }

    @Test
    void listingIncludesOnlyNextCalendarWeekEventsInChronologicalOrder() {
        Activite activity = new Activite();
        activity.setId(21L);
        activity.setNom("Basket");
        Equipe team = new Equipe();
        team.setId(3L);
        team.setNom("U15");
        team.setActivite(activity);
        Equipe otherTeam = new Equipe();
        otherTeam.setId(4L);
        otherTeam.setNom("U18");
        otherTeam.setActivite(activity);
        var monday = LocalDate.now(ZoneId.of("Europe/Paris"))
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        var starts = List.of(monday.minusSeconds(1), monday, monday.plusDays(6).plusHours(23),
                monday.plusWeeks(1), monday.plusDays(2));
        for (int index = 0; index < starts.size(); index++) {
            Seance event = new Seance();
            event.setId((long) index + 1);
            event.setActivite(activity);
            event.setDebut(starts.get(index));
            event.setFin(starts.get(index).plusHours(1));
            event.setEtatSeance(index == 4 ? ESeance.ANNULEE : ESeance.PROGRAMMEE);
            event.setTypeEvenement("RENCONTRE");
            event.setAdversaire("Club voisin");
            event.setDescriptif("Championnat");
            event.getEquipes().addAll(List.of(otherTeam, team));
            activity.getSeances().add(0, event);
        }
        activity.getSeances().add(seanceAvecEtat(ESeance.PROGRAMMEE));
        when(activiteRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity)));

        var result = activiteServices.getPage("", null, null, null, null, "", PageRequest.of(0, 20))
                .getContent().getFirst().getEvenementsSemaineProchaine();

        assertThat(result).extracting(event -> event.id()).containsExactly(2L, 3L);
        assertThat(result.getFirst().descriptif()).isEqualTo("U15 / U18");
        assertThat(result.getFirst().nomEvenement()).isEqualTo("Championnat");
        assertThat(result.getFirst().typeEvenement()).isEqualTo("RENCONTRE");
        assertThat(result.getFirst().adversaire()).isEqualTo("Club voisin");
    }

    @Test
    void searchesByAllActivityFields() {
        Pageable pageable = PageRequest.of(0, 20);
        when(activiteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        activiteServices.getPage("Danse", 200, true, false, 16, "Féminine", pageable);

        verify(activiteRepository)
                .findAll(any(Specification.class), eq(PageRequest.of(0, 20,
                        Sort.by(Sort.Direction.ASC, "nom", "id"))));
    }

    private Adhesion adhesionAvecStatut(Status status) {
        Adhesion adhesion = new Adhesion();
        adhesion.setId((long) status.ordinal() + 1);
        adhesion.setStatutActuel(status.label);
        return adhesion;
    }

    private Seance seanceAvecEtat(ESeance etat) {
        Seance seance = new Seance();
        seance.setId((long) etat.ordinal() + 1);
        seance.setEtatSeance(etat);
        return seance;
    }

}
