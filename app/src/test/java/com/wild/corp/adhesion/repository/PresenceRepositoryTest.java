package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Presence;
import com.wild.corp.adhesion.models.Seance;
import com.wild.corp.adhesion.models.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = SeanceRepositoryTest.JpaTestApplication.class)
class PresenceRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private PresenceRepository repository;

    @Test
    void findsAttendanceOnlyForTheRequestedSessionAndAuthenticatedMemberAndPersistsThePlannedValue() {
        Seance first = new Seance();
        Seance second = new Seance();
        entityManager.persist(first);
        entityManager.persist(second);
        Presence own = participant(first, "alice@example.org");
        Presence other = participant(first, "paul@example.org");
        entityManager.flush();
        entityManager.clear();

        Presence found = repository.findBySeance_IdAndAdhesion_Adherent_User_Username(first.getId(), "alice@example.org")
                .orElseThrow();
        assertThat(found.getId()).isEqualTo(own.getId());
        assertThat(repository.findBySeance_IdAndAdhesion_Adherent_User_Username(first.getId(), "unknown@example.org")).isEmpty();
        assertThat(repository.findBySeance_IdAndAdhesion_Adherent_User_Username(second.getId(), "alice@example.org")).isEmpty();
        found.setPresencePrevue(true);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(own.getId()).orElseThrow().getPresencePrevue()).isTrue();
        assertThat(repository.findById(own.getId()).orElseThrow().getPresence()).isNull();
        assertThat(repository.findById(other.getId()).orElseThrow().getPresencePrevue()).isNull();
    }

    private Presence participant(Seance seance, String username) {
        Adherent adherent = new Adherent();
        adherent.setUser(new User(username, "password"));
        entityManager.persist(adherent);
        Adhesion adhesion = new Adhesion();
        adhesion.setAdherent(adherent);
        entityManager.persist(adhesion);
        Presence presence = new Presence();
        presence.setSeance(seance);
        presence.setAdhesion(adhesion);
        entityManager.persist(presence);
        return presence;
    }
}
