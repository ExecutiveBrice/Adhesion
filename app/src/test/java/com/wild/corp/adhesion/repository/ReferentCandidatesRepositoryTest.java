package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = AdherentRoleRepositoryTest.JpaTestApplication.class)
class ReferentCandidatesRepositoryTest {
    @Autowired private EntityManager em;
    @Autowired private AdherentRepository adherents;

    @Test
    void includesEveryTribeMemberForAnyNonCancelledNonWaitingSectionMembership() {
        Section section = Section.builder().nom("Basket").type("COMPETITION").build();
        em.persist(section);
        Activite activity = new Activite();
        activity.setNom("U11");
        activity.setSection(section);
        em.persist(activity);

        Tribu registeredTribe = tribe();
        Adherent member = member("Inscrit", registeredTribe);
        Adherent sibling = member("Fratrie", registeredTribe);
        membership(member, activity, Status.ATTENTE_SECRETARIAT);
        Adherent pending = member("Sans statut", tribe());
        Adhesion pendingMembership = new Adhesion();
        pendingMembership.setAdherent(pending);
        pendingMembership.setActivite(activity);
        em.persist(pendingMembership);

        Tribu waitingTribe = tribe();
        member("Autre", waitingTribe);
        membership(member("Attente", waitingTribe), activity, Status.LISTE_ATTENTE);
        Tribu cancelledTribe = tribe();
        membership(member("Annulé", cancelledTribe), activity, Status.ANNULEE);
        em.flush();
        em.clear();

        assertThat(adherents.findReferentCandidates(section.getId(),
                List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label)))
                .extracting(Adherent::getId).containsExactlyInAnyOrder(member.getId(), sibling.getId(), pending.getId());
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

    private void membership(Adherent member, Activite activity, Status status) {
        Adhesion membership = new Adhesion();
        membership.setAdherent(member);
        membership.setActivite(activity);
        membership.setStatutActuel(status.label);
        em.persist(membership);
    }
}
