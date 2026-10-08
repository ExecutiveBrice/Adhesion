package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = AdherentRoleRepositoryTest.JpaTestApplication.class)
class SectionListingSpecificationsTest {
    @Autowired private EntityManager em;
    @Autowired private AdhesionRepository adhesions;
    @Autowired private AdherentRepository adherents;
    @Autowired private ActiviteRepository activites;

    @Test
    void restrictsMembershipsToManagedSectionsAndMembersToTheirTribes() {
        Section managed = section("Basket");
        Section other = section("Danse");
        Activite basket = activity(managed);
        Activite danse = activity(other);

        Tribu family = tribe();
        Adherent registered = member("Inscrit", family);
        Adherent sibling = member("Fratrie", family);
        Adhesion managedMembership = membership(registered, basket);
        Tribu anotherFamily = tribe();
        Adherent outside = member("Externe", anotherFamily);
        membership(outside, danse);
        em.flush();
        em.clear();

        assertThat(adhesions.findAll(SectionListingSpecifications.adhesions(Set.of(managed.getId()))))
                .extracting(Adhesion::getId).containsExactly(managedMembership.getId());
        assertThat(adherents.findAll(SectionListingSpecifications.membresDesTribus(Set.of(managed.getId()))))
                .extracting(Adherent::getId).containsExactlyInAnyOrder(registered.getId(), sibling.getId());
        assertThat(activites.findAll(SectionListingSpecifications.activites(Set.of(managed.getId()))))
                .extracting(Activite::getId).containsExactly(basket.getId());
    }

    private Section section(String name) {
        Section section = Section.builder().nom(name).type("NON_COMPETITIVE").build();
        em.persist(section);
        return section;
    }

    private Activite activity(Section section) {
        Activite activity = new Activite();
        activity.setNom(section.getNom());
        activity.setSection(section);
        em.persist(activity);
        return activity;
    }

    private Tribu tribe() {
        Tribu tribe = new Tribu(UUID.randomUUID());
        em.persist(tribe);
        return tribe;
    }

    private Adherent member(String name, Tribu tribe) {
        Adherent member = new Adherent();
        member.setNom(name);
        member.setPrenom("Test");
        member.setTribu(tribe);
        em.persist(member);
        return member;
    }

    private Adhesion membership(Adherent member, Activite activity) {
        Adhesion membership = new Adhesion();
        membership.setAdherent(member);
        membership.setActivite(activity);
        membership.setStatutActuel("Annulée");
        em.persist(membership);
        return membership;
    }
}
