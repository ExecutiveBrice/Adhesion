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

    @Test
    void coachedActivitiesAreDistinctPaginatedAndLimitedToTheAccountsActiveTeams() {
        User account = new User("coach@example.org", "password");
        account.getRoles().add(ERole.ROLE_ENCADRANT);
        em.persist(account);
        Adherent coach = member("Encadrant", tribe());
        coach.setUser(account);
        Adherent represented = member("Enfant", coach.getTribu());
        represented.setRepresentant(coach);
        Section section = section("Basket");
        Activite coached = activity(section);
        Activite sameSection = activity(section);
        Activite archived = activity(section);
        Activite referred = activity(section);
        Activite childsTeam = activity(section);
        team(coached, coach, false);
        team(coached, coach, false);
        team(sameSection, represented, false);
        team(archived, coach, true);
        Equipe referredTeam = team(referred, represented, false);
        referredTeam.getReferents().add(coach);
        team(childsTeam, represented, false);
        em.flush();
        em.clear();

        var scope = SectionListingSpecifications.activitesEncadrees(account.getUsername());
        var page = activites.findAll(scope, org.springframework.data.domain.PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Activite::getId).containsExactly(coached.getId());
        assertThat(activites.findAll(scope.and((root, query, builder) ->
                builder.equal(root.get("id"), sameSection.getId())))).isEmpty();
        assertThat(activites.findAll(SectionListingSpecifications.activitesEncadrees("other@example.org"))).isEmpty();
        assertThat(activites.findAll(SectionListingSpecifications.activites(Set.of(section.getId())).or(scope)))
                .extracting(Activite::getId).containsExactlyInAnyOrder(coached.getId(), sameSection.getId(),
                        archived.getId(), referred.getId(), childsTeam.getId());
    }

    private Equipe team(Activite activity, Adherent coach, boolean archived) {
        Equipe team = new Equipe();
        team.setActivite(activity);
        team.setNom(UUID.randomUUID().toString());
        team.setArchivee(archived);
        team.getEncadrants().add(coach);
        em.persist(team);
        return team;
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
