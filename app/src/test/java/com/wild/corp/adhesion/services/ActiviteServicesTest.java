package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.PlanificationHebdomadaire;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.repository.SectionRepository;
import com.wild.corp.adhesion.repository.ActiviteRepository;
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

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SeanceServices seanceServices;

    @Mock
    private AdherentServices adherentServices;

    @Test
    void resolvesSectionFromDatabaseWhenSavingActivity() {
        Activite activite = new Activite();
        activite.setSection(Section.builder().id(7L).nom("Nom envoyé").type("COMPETITION").build());
        Section section = Section.builder().id(7L).nom("Yoga").type("NON_COMPETITIVE").build();
        when(sectionRepository.findById(7L)).thenReturn(Optional.of(section));
        when(activiteRepository.save(activite)).thenReturn(activite);

        Activite resultat = activiteServices.save(activite);

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
        org.mockito.Mockito.verifyNoInteractions(activiteRepository, sectionRepository, seanceServices);
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
        assertThat(activite.getNbSeancesRealisees()).isEqualTo(1);
        assertThat(activite.getNbSeancesTotal()).isEqualTo(2);

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
    void searchesByAllActivityFields() {
        Pageable pageable = PageRequest.of(0, 20);
        when(activiteRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        activiteServices.getPage("Danse", 200, true, false, 16, "Féminine", pageable);

        verify(activiteRepository)
                .findAll(any(Specification.class), eq(PageRequest.of(0, 20,
                        Sort.by(Sort.Direction.ASC, "nom", "id"))));
    }

    @Test
    void associatesSessionCountersWithTheirWeeklyCategory() {
        Activite activite = new Activite();
        PlanificationHebdomadaire lundi = planification(1L);
        PlanificationHebdomadaire mercredi = planification(2L);
        activite.getPlanificationsHebdomadaires().addAll(List.of(lundi, mercredi));
        activite.getSeances().add(seanceAvecEtatEtPlanification(ESeance.PROGRAMMEE, lundi));
        activite.getSeances().add(seanceAvecEtatEtPlanification(ESeance.REALISEE, lundi));
        activite.getSeances().add(seanceAvecEtatEtPlanification(ESeance.ANNULEE, lundi));
        activite.getSeances().add(seanceAvecEtatEtPlanification(ESeance.REALISEE, mercredi));

        when(activiteRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activite)));

        activiteServices.getPage("", null, null, null, null, "", PageRequest.of(0, 20));

        assertThat(lundi.getNbSeancesTotal()).isEqualTo(2);
        assertThat(lundi.getNbSeancesRealisees()).isEqualTo(1);
        assertThat(mercredi.getNbSeancesTotal()).isEqualTo(1);
        assertThat(mercredi.getNbSeancesRealisees()).isEqualTo(1);
    }

    @Test
    void updatesAnActivityWithoutReplacingOrDeletingItsSessions() {
        Activite activiteInDB = new Activite();
        activiteInDB.setId(12L);
        activiteInDB.setNom("Ancien nom");
        activiteInDB.setJour(DayOfWeek.TUESDAY);

        Seance seance = new Seance();
        seance.setId(31L);
        seance.setActivite(activiteInDB);
        seance.setDebut(LocalDateTime.of(2026, 9, 1, 8, 15));
        seance.setFin(LocalDateTime.of(2026, 9, 1, 9, 0));
        activiteInDB.getSeances().add(seance);

        Activite modification = new Activite();
        modification.setId(12L);
        modification.setNom("Nouveau nom");
        modification.setJour(DayOfWeek.WEDNESDAY);
        Section section = Section.builder().id(7L).nom("Yoga").type("NON_COMPETITIVE").build();
        modification.setSection(section);
        when(sectionRepository.findById(7L)).thenReturn(Optional.of(section));

        when(activiteRepository.findById(12L)).thenReturn(Optional.of(activiteInDB));
        when(activiteRepository.save(activiteInDB)).thenReturn(activiteInDB);

        Activite resultat = activiteServices.save(modification);

        assertThat(resultat.getNom()).isEqualTo("Nouveau nom");
        assertThat(resultat.getJour()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(resultat.getSeances()).containsExactly(seance);
        verify(seanceServices, never()).modifyDay(activiteInDB);
        verify(activiteRepository).save(activiteInDB);
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

    private Seance seanceAvecEtatEtPlanification(ESeance etat, PlanificationHebdomadaire planification) {
        Seance seance = seanceAvecEtat(etat);
        seance.setPlanification(planification);
        return seance;
    }

    private PlanificationHebdomadaire planification(Long id) {
        PlanificationHebdomadaire planification = new PlanificationHebdomadaire();
        planification.setId(id);
        return planification;
    }
}
