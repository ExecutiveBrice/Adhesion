package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Activite;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.Section;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = SeanceRepositoryTest.JpaTestApplication.class)
class SeanceRepositoryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Seance.class)
    @EnableJpaRepositories(basePackageClasses = SeanceRepository.class)
    static class JpaTestApplication {}

    @Autowired private EntityManager entityManager;
    @Autowired private SeanceRepository seanceRepository;
    @Autowired private ActiviteRepository activiteRepository;
    @Autowired private ConvocationEquipeRepository invitations;

    @Test void pendingRepliesExcludeOtherPeopleUnpublishedWithdrawnAnsweredAndPastInvitations() {
        var user = new com.wild.corp.adhesion.models.User("alice@example.org", "password");
        entityManager.persist(user);
        var person = new com.wild.corp.adhesion.models.Adherent(); person.setUser(user);
        entityManager.persist(person);
        var other = new com.wild.corp.adhesion.models.Adherent(); entityManager.persist(other);
        var now = LocalDateTime.now();
        var future = session(now.plusDays(30), ESeance.PROGRAMMEE);
        var modified = session(now.plusDays(2), ESeance.MODIFIEE);
        var withoutInvitations = session(now.plusDays(1), ESeance.PROGRAMMEE);
        withoutInvitations.setSansInvitation(true);
        for (int i = 0; i < 12; i++) {
            var c = new com.wild.corp.adhesion.models.ConvocationEquipe();
            c.setSeance(i == 0 ? modified : i == 7 ? session(now.minusHours(1), ESeance.PROGRAMMEE)
                    : i == 8 ? session(now.plusDays(1), ESeance.ANNULEE)
                    : i == 9 ? session(now.plusDays(1), ESeance.REALISEE)
                    : i >= 10 ? withoutInvitations : future);
            c.setAdherent(i == 2 ? other : person);
            c.setObjet(i == 0 || i == 10 ? "PARTICIPANT" : "RESPONSABILITE:" + i + ":0");
            c.setSelectionnee(i != 3);
            c.setPublication(i == 4 ? null : now.minusDays(1));
            if (i == 5) c.setReponse(false);
            if (i == 6) c.setDateReponse(now); // Explicit “not sure yet” is already a reply.
            entityManager.persist(c);
        }
        entityManager.flush(); entityManager.clear();
        assertThat(invitations.findPendingForUser(user.getUsername(), now,
                List.of(ESeance.PROGRAMMEE, ESeance.MODIFIEE)))
                .extracting(c -> c.getSeance().getId()).containsExactly(withoutInvitations.getId(), modified.getId(), future.getId());
    }

    @Test
    void explicitlySavedSessionsRemainAvailableAfterReload() {
        Section section = Section.builder().nom("Basket").type("COMPETITION").build();
        entityManager.persist(section);
        Activite activity = new Activite();
        activity.setNom("Senior F");
        activity.setSection(section);
        entityManager.persist(activity);
        entityManager.flush();
        entityManager.clear();

        Activite managed = activiteRepository.findById(activity.getId()).orElseThrow();
        Seance session = new Seance();
        session.setActivite(managed);
        session.setDebut(LocalDateTime.of(2026, 10, 12, 20, 45));
        managed.getSeances().add(session);
        seanceRepository.saveAllAndFlush(List.of(session));
        entityManager.flush();
        entityManager.clear();

        assertThat(seanceRepository.findByActivite_IdOrderByDebutAsc(activity.getId()))
                .extracting(Seance::getDebut).containsExactly(LocalDateTime.of(2026, 10, 12, 20, 45));
    }

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

    private Seance session(LocalDateTime debut, ESeance etat) {
        Seance seance = new Seance();
        seance.setDebut(debut);
        seance.setEtatSeance(etat);
        entityManager.persist(seance);
        return seance;
    }
}
