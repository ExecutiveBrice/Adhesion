package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.resources.AgendaPreferences;
import com.wild.corp.adhesion.services.AgendaPreferencesServices;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = AdherentRoleRepositoryTest.JpaTestApplication.class)
class AgendaPreferencesRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private AdherentRepository adherents;

    @Test
    void persistsSelectionForConnectedMemberAndCanSelectEverythingAgain() {
        for (String username : Set.of("alice@example.test", "bob@example.test")) {
            var adherent = new Adherent();
            adherent.setUser(new User(username, "encoded-password"));
            entityManager.persist(adherent);
        }
        entityManager.flush();
        entityManager.clear();
        var service = new AgendaPreferencesServices(adherents);
        var selection = new AgendaPreferences(Set.of(10L, 20L), Set.of(8L));
        assertThat(service.get("alice@example.test")).isEqualTo(new AgendaPreferences(Set.of(), Set.of()));
        service.update("alice@example.test", selection);
        entityManager.flush();
        entityManager.clear();

        assertThat(service.get("alice@example.test")).isEqualTo(selection);
        assertThat(service.get("bob@example.test")).isEqualTo(new AgendaPreferences(Set.of(), Set.of()));
        service.update("alice@example.test", new AgendaPreferences(Set.of(), Set.of()));
        entityManager.flush();
        entityManager.clear();
        assertThat(service.get("alice@example.test")).isEqualTo(new AgendaPreferences(Set.of(), Set.of()));
    }

    @Test
    void refusesAnAccountWithoutAnAssociatedMember() {
        assertThatThrownBy(() -> new AgendaPreferencesServices(adherents).get("unknown@example.test"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(404);
    }
}
