package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ERole;
import com.wild.corp.adhesion.models.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ContextConfiguration(classes = AdherentRoleRepositoryTest.JpaTestApplication.class)
class AdherentRoleRepositoryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = AdherentRepository.class)
    static class JpaTestApplication {}

    @Autowired private EntityManager entityManager;
    @Autowired private AdherentRepository adherentRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void findsAdherentByDirectEnumRole() {
        User professor = new User("prof@example.test", "encoded-password");
        professor.getRoles().add(ERole.ROLE_USER);
        professor.getRoles().add(ERole.ROLE_ENCADRANT);
        Adherent adherent = new Adherent();
        adherent.setPrenom("Alice");
        adherent.setUser(professor);
        entityManager.persist(adherent);
        entityManager.flush();
        entityManager.clear();

        assertThat(adherentRepository.findByUserRole(ERole.ROLE_ENCADRANT))
                .extracting(Adherent::getPrenom).containsExactly("Alice");
        assertThat(adherentRepository.findByUserRole(ERole.ROLE_BUREAU)).isEmpty();
    }

    @Test
    void persistsGrantAndRevocationOfEnumRole() {
        User user = new User("member@example.test", "encoded-password");
        user.getRoles().add(ERole.ROLE_USER);
        Adherent adherent = new Adherent();
        adherent.setPrenom("Bob");
        adherent.setUser(user);
        entityManager.persist(adherent);
        entityManager.flush();
        entityManager.clear();

        User storedUser = userRepository.findByUsername("member@example.test").orElseThrow();
        storedUser.getRoles().add(ERole.ROLE_ENCADRANT);
        entityManager.flush();
        entityManager.clear();
        assertThat(adherentRepository.findByUserRole(ERole.ROLE_ENCADRANT))
                .extracting(Adherent::getPrenom).containsExactly("Bob");

        storedUser = userRepository.findByUsername("member@example.test").orElseThrow();
        storedUser.getRoles().remove(ERole.ROLE_ENCADRANT);
        entityManager.flush();
        entityManager.clear();
        assertThat(adherentRepository.findByUserRole(ERole.ROLE_ENCADRANT)).isEmpty();
    }
}
