package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.ESeance;
import com.wild.corp.adhesion.models.Seance;
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

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ContextConfiguration(classes = SeanceRepositoryTest.JpaTestApplication.class)
class SeanceRepositoryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Seance.class)
    @EnableJpaRepositories(basePackageClasses = SeanceRepository.class)
    static class JpaTestApplication {}

    @Autowired private EntityManager entityManager;
    @Autowired private SeanceRepository seanceRepository;

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
