package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.resources.SeanceResponse;
import com.wild.corp.adhesion.services.AccordServices;
import com.wild.corp.adhesion.services.ActiviteServices;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.AdhesionServices;
import com.wild.corp.adhesion.services.EmailService;
import com.wild.corp.adhesion.services.PdfService;
import com.wild.corp.adhesion.services.PresenceServices;
import com.wild.corp.adhesion.services.SeanceServices;
import com.wild.corp.adhesion.services.UserServices;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Records;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = SeanceRepositoryTest.JpaTestApplication.class)
@Import({ActiviteServices.class, AdhesionServices.class, PresenceServices.class, SeanceServices.class})
@MockitoBean(types = {EmailService.class, PdfService.class, UserServices.class})
class SeanceRepositoryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Seance.class)
    @EnableJpaRepositories(basePackageClasses = SeanceRepository.class)
    static class JpaTestApplication {}

    @Autowired private EntityManager entityManager;
    @Autowired private SeanceRepository seanceRepository;
    @Autowired private AdhesionServices adhesionServices;
    @Autowired private ActiviteServices activiteServices;
    @Autowired private SeanceServices seanceServices;
    @Autowired private PresenceRepository presenceRepository;
    @MockitoBean private AdherentServices adherentServices;
    @MockitoBean private AccordServices accordServices;
    @MockitoBean(name = "vacancesDatasetApi") private DatasetApi vacancesApi;
    @MockitoBean(name = "joursFeriesDatasetApi") private DatasetApi joursFeriesApi;

    @Test
    void catchesUpPastSessionsWithoutChangingCanceledOrCurrentSessions() {
        Seance oldScheduled = session(LocalDateTime.of(2026, 9, 12, 10, 0), ESeance.PROGRAMMEE);
        Seance yesterdayModified = session(LocalDateTime.of(2026, 9, 14, 18, 0), ESeance.MODIFIEE);
        Seance canceled = session(LocalDateTime.of(2026, 9, 13, 10, 0), ESeance.ANNULEE);
        Seance today = session(LocalDateTime.of(2026, 9, 15, 0, 0), ESeance.PROGRAMMEE);
        Seance tomorrow = session(LocalDateTime.of(2026, 9, 16, 10, 0), ESeance.PROGRAMMEE);
        entityManager.flush();
        entityManager.clear();

        int updated = seanceRepository.updateEtatForDebutBeforeAndEtatIn(
                LocalDateTime.of(2026, 9, 15, 0, 0),
                List.of(ESeance.PROGRAMMEE, ESeance.MODIFIEE), ESeance.REALISEE);

        assertThat(updated).isEqualTo(2);
        assertThat(seanceRepository.findById(oldScheduled.getId()).orElseThrow().getEtatSeance()).isEqualTo(ESeance.REALISEE);
        assertThat(seanceRepository.findById(yesterdayModified.getId()).orElseThrow().getEtatSeance()).isEqualTo(ESeance.REALISEE);
        assertThat(seanceRepository.findById(canceled.getId()).orElseThrow().getEtatSeance()).isEqualTo(ESeance.ANNULEE);
        assertThat(seanceRepository.findById(today.getId()).orElseThrow().getEtatSeance()).isEqualTo(ESeance.PROGRAMMEE);
        assertThat(seanceRepository.findById(tomorrow.getId()).orElseThrow().getEtatSeance()).isEqualTo(ESeance.PROGRAMMEE);
    }

    @Test
    void registeringForAnActivityConnectsTheMemberToExistingAndNewSessions() {
        Activite activite = activity();
        activite.setNbPlaces(10);
        activite.setJour(DayOfWeek.MONDAY);
        activite.setHoraireDebut(LocalTime.of(18, 30));
        activite.setDuree(60L);
        Seance reportee = session(activite, ESeance.MODIFIEE);
        reportee.setDebut(LocalDateTime.of(2026, 9, 7, 18, 30));
        Adherent adherent = new Adherent();
        Tribu tribu = new Tribu();
        entityManager.persist(tribu);
        adherent.setTribu(tribu);
        adherent.setUser(new User("adherent@example.test", "hash"));
        entityManager.persist(adherent);
        entityManager.flush();
        entityManager.clear();

        when(adherentServices.getById(adherent.getId()))
                .thenAnswer(invocation -> entityManager.find(Adherent.class, adherent.getId()));
        when(adherentServices.hasValidatedAccords(any(Adherent.class))).thenReturn(true);
        when(accordServices.createAccord(anyString(), isNull(), any(Adhesion.class))).thenAnswer(invocation -> {
            Accord accord = new Accord();
            accord.setNom(invocation.getArgument(0));
            accord.setAdhesion(invocation.getArgument(2));
            return accord;
        });
        for (DatasetApi api : List.of(vacancesApi, joursFeriesApi)) {
            when(api.getRecords(anyString(), isNull(), anyString(), isNull(), isNull(), eq(100), eq(0),
                    isNull(), isNull(), eq("fr"), eq("Europe/Paris"), eq(false), eq(false)))
                    .thenReturn(ResponseEntity.ok(new Records().results(List.of())));
        }

        Adhesion adhesion = adhesionServices.save(() -> "adherent@example.test", adherent.getId(), activite.getId());
        adhesionServices.saveUnique(adhesion);
        entityManager.flush();
        entityManager.clear();
        assertThat(presences(reportee.getId())).extracting(presence -> presence.getAdhesion().getId())
                .containsExactly(adhesion.getId());

        List<SeanceResponse> seances = activiteServices.addSeances(activite.getId(), 1, LocalDate.of(2026, 9, 14));
        entityManager.flush();
        entityManager.clear();

        assertThat(seances).hasSize(2);
        Long nouvelleSeanceId = seances.getLast().id();
        assertThat(presences(nouvelleSeanceId)).extracting(presence -> presence.getAdhesion().getId())
                .containsExactly(adhesion.getId());
    }

    @Test
    void transferringAnAdhesionReplacesItsFuturePresencesAndKeepsItsHistory() {
        LocalDateTime maintenant = LocalDateTime.now(ZoneId.of("Europe/Paris"));
        Activite ancienne = activity();
        Activite nouvelle = activity();
        Adhesion adhesion = adhesion(ancienne, Status.VALIDEE);
        Seance historique = session(ancienne, ESeance.REALISEE);
        historique.setDebut(maintenant.minusDays(1));
        Presence presenceHistorique = presence(adhesion, historique);
        Seance ancienneSeanceAVenir = session(ancienne, ESeance.MODIFIEE);
        ancienneSeanceAVenir.setDebut(maintenant.plusDays(1));
        Presence anciennePresenceAVenir = presence(adhesion, ancienneSeanceAVenir);
        Adhesion autreAdhesion = adhesion(ancienne, Status.VALIDEE);
        presence(autreAdhesion, ancienneSeanceAVenir);
        Seance programmee = session(nouvelle, ESeance.PROGRAMMEE);
        programmee.setDebut(maintenant.plusDays(2));
        Presence presenceDestination = presence(adhesion, programmee);
        Seance reportee = session(nouvelle, ESeance.MODIFIEE);
        reportee.setDebut(maintenant.plusDays(3));
        Seance annulee = session(nouvelle, ESeance.ANNULEE);
        Seance terminee = session(nouvelle, ESeance.REALISEE);
        entityManager.flush();
        entityManager.clear();

        adhesionServices.changeActivite(adhesion.getId(), nouvelle.getId());
        adhesionServices.changeActivite(adhesion.getId(), nouvelle.getId());
        entityManager.flush();
        entityManager.clear();

        Adhesion rechargee = entityManager.find(Adhesion.class, adhesion.getId());
        assertThat(rechargee.getActivite().getId()).isEqualTo(nouvelle.getId());
        assertThat(rechargee.getPresences()).extracting(Presence::getSeanceId)
                .containsExactlyInAnyOrder(historique.getId(), programmee.getId(), reportee.getId());
        assertThat(entityManager.find(Presence.class, presenceHistorique.getId()).getPresence()).isTrue();
        assertThat(entityManager.find(Presence.class, presenceDestination.getId()).getPresence()).isTrue();
        assertThat(entityManager.find(Presence.class, anciennePresenceAVenir.getId())).isNull();
        assertThat(presences(ancienneSeanceAVenir.getId())).extracting(presence -> presence.getAdhesion().getId())
                .containsExactly(autreAdhesion.getId());
        assertThat(presences(annulee.getId())).isEmpty();
        assertThat(presences(terminee.getId())).isEmpty();
    }

    @Test
    void cancellingAnAdhesionDeletesOnlyItsFuturePresences() {
        LocalDateTime maintenant = LocalDateTime.now(ZoneId.of("Europe/Paris"));
        Activite activite = activity();
        Adhesion adhesion = adhesion(activite, Status.VALIDEE);
        Adhesion autreAdhesion = adhesion(activite, Status.VALIDEE);
        Seance commencee = session(activite, ESeance.PROGRAMMEE);
        commencee.setDebut(maintenant.minusHours(1));
        Presence presenceHistorique = presence(adhesion, commencee);
        Seance aVenir = session(activite, ESeance.PROGRAMMEE);
        aVenir.setDebut(maintenant.plusHours(1));
        Presence presenceAVenir = presence(adhesion, aVenir);
        presence(autreAdhesion, aVenir);
        entityManager.flush();
        entityManager.clear();

        adhesionServices.choisirStatut(adhesion.getId(), Status.ANNULEE.label);
        seanceServices.updateCommentaireForSecretary(aVenir.getId(), "Prochaine séance");
        entityManager.flush();
        entityManager.clear();

        Adhesion annulee = entityManager.find(Adhesion.class, adhesion.getId());
        assertThat(annulee.getStatutActuel()).isEqualTo(Status.ANNULEE.label);
        assertThat(annulee.getPresences()).extracting(Presence::getId).containsExactly(presenceHistorique.getId());
        assertThat(entityManager.find(Presence.class, presenceAVenir.getId())).isNull();
        assertThat(presences(aVenir.getId())).extracting(presence -> presence.getAdhesion().getId())
                .containsExactly(autreAdhesion.getId());
    }

    @Test
    void editingAnExistingSessionFillsOnlyMissingPresences() {
        Activite activite = activity();
        Seance seance = session(activite, ESeance.PROGRAMMEE);
        Adhesion connue = adhesion(activite, Status.VALIDEE);
        Adhesion nouvelle = adhesion(activite, Status.ATTENTE_ADHERENT);
        adhesion(activite, Status.LISTE_ATTENTE);
        adhesion(activite, Status.ANNULEE);
        Presence enregistree = presence(connue, seance);
        entityManager.flush();
        entityManager.clear();

        seanceServices.updateSeance(activite.getId(), seance.getId(), ESeance.MODIFIEE,
                null, false, null, null, false, null, false);
        seanceServices.updateCommentaireForSecretary(seance.getId(), "Reportée");
        entityManager.flush();
        entityManager.clear();

        assertThat(presences(seance.getId())).extracting(presence -> presence.getAdhesion().getId())
                .containsExactlyInAnyOrder(connue.getId(), nouvelle.getId());
        Presence conservee = entityManager.find(Presence.class, enregistree.getId());
        assertThat(conservee.getPresence()).isTrue();
        assertThat(conservee.getPresencePrevue()).isFalse();
    }

    private Activite activity() {
        Activite activite = new Activite();
        entityManager.persist(activite);
        return activite;
    }

    private Adhesion adhesion(Activite activite, Status statut) {
        Adhesion adhesion = new Adhesion();
        adhesion.setActivite(activite);
        adhesion.setStatutActuel(statut.label);
        entityManager.persist(adhesion);
        return adhesion;
    }

    private Presence presence(Adhesion adhesion, Seance seance) {
        Presence presence = new Presence();
        presence.setAdhesion(adhesion);
        presence.setSeance(seance);
        presence.setPresence(true);
        presence.setPresencePrevue(false);
        entityManager.persist(presence);
        return presence;
    }

    private List<Presence> presences(Long seanceId) {
        return presenceRepository.findBySeance_IdOrderByAdhesion_Adherent_NomAscAdhesion_Adherent_PrenomAsc(seanceId);
    }

    private Seance session(Activite activite, ESeance etat) {
        Seance seance = new Seance();
        seance.setActivite(activite);
        seance.setEtatSeance(etat);
        entityManager.persist(seance);
        return seance;
    }

    private Seance session(LocalDateTime debut, ESeance etat) {
        Seance seance = new Seance();
        seance.setDebut(debut);
        seance.setEtatSeance(etat);
        entityManager.persist(seance);
        return seance;
    }
}
