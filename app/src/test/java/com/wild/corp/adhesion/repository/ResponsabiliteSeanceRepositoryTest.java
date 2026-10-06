package com.wild.corp.adhesion.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = SeanceRepositoryTest.JpaTestApplication.class)
class ResponsabiliteSeanceRepositoryTest {
    @Autowired EntityManager em;
    @Autowired SeanceRepository seances;
    @Autowired AdherentRepository adherents;
    @Autowired TacheSeanceRepository taches;

    @Test
    void persistsReplacesAndClearsAssignmentsAndDeletesThemWithTheSession() throws Exception {
        TacheSeance tache = new TacheSeance();
        tache.setNom("Matériel");
        em.persist(tache);
        Adherent alice = new Adherent();
        alice.setPrenom("Alice");
        alice.setNom("Dupont");
        alice.setTelephone("privé");
        em.persist(alice);
        Adherent bob = new Adherent();
        bob.setPrenom("Bob");
        em.persist(bob);
        Seance seance = new Seance();
        seance.getResponsabilites().add(new ResponsabiliteSeance(tache, alice));
        seance.getResponsabilites().add(new ResponsabiliteSeance(tache, bob));
        em.persist(seance);
        em.flush();
        em.clear();
        Seance saved = seances.findById(seance.getId()).orElseThrow();
        assertThat(saved.getResponsabilites()).hasSize(2);
        var json = new ObjectMapper().valueToTree(saved.getResponsabilites().getFirst());
        assertThat(json.path("adherent").path("prenom").asText()).isEqualTo("Alice");
        assertThat(json.path("adherent").size()).isEqualTo(3);
        assertThat(json.path("adherent").has("telephone")).isFalse();
        assertThat(taches.isUsed(tache.getId())).isTrue();
        var premiere = saved.getResponsabilites().removeFirst();
        saved.getResponsabilites().add(premiere);
        em.flush();
        em.clear();
        saved = seances.findById(seance.getId()).orElseThrow();
        assertThat(saved.getResponsabilites().getFirst().getAdherent().getId()).isEqualTo(bob.getId());
        saved.getResponsabilites().clear();
        em.flush();
        assertThat(taches.isUsed(tache.getId())).isFalse();
        saved.getResponsabilites().add(new ResponsabiliteSeance(em.find(TacheSeance.class, tache.getId()), em.find(Adherent.class, alice.getId())));
        em.flush();
        seances.delete(saved);
        em.flush();
        assertThat(taches.isUsed(tache.getId())).isFalse();
        assertThat(taches.existsById(tache.getId())).isTrue();
        assertThat(adherents.existsById(alice.getId())).isTrue();
    }

    @Test
    void candidatesIncludeMembersOfOtherActivitiesInTheSectionButExcludeCanceledAndWaitingRegistrations() {
        Section yoga = section("Yoga");
        Section basket = section("Basket");
        Activite matin = activite(yoga);
        Activite soir = activite(yoga);
        Activite autre = activite(basket);
        Adherent alice = membre(matin, Status.VALIDEE.label);
        Adhesion secondeAdhesion = new Adhesion();
        secondeAdhesion.setAdherent(alice);
        secondeAdhesion.setActivite(soir);
        secondeAdhesion.setStatutActuel(Status.VALIDEE.label);
        em.persist(secondeAdhesion);
        Adherent bob = membre(soir, Status.VALIDEE.label);
        membre(autre, Status.VALIDEE.label);
        membre(matin, Status.ANNULEE.label);
        membre(matin, Status.LISTE_ATTENTE.label);
        em.flush();
        var exclus = List.of(Status.ANNULEE.label, Status.LISTE_ATTENTE.label);
        assertThat(adherents.findResponsabiliteCandidates(yoga.getId(), exclus))
                .extracting(Adherent::getId).containsExactlyInAnyOrder(alice.getId(), bob.getId());
        assertThat(adherents.findResponsabiliteCandidate(bob.getId(), yoga.getId(), exclus)).isPresent();
        assertThat(adherents.findResponsabiliteCandidate(bob.getId(), basket.getId(), exclus)).isEmpty();
    }

    private Section section(String nom) {
        Section section = new Section();
        section.setNom(nom);
        section.setType("NON_COMPETITIVE");
        em.persist(section);
        return section;
    }

    private Activite activite(Section section) {
        Activite activite = new Activite();
        activite.setSection(section);
        em.persist(activite);
        return activite;
    }

    private Adherent membre(Activite activite, String statut) {
        Adherent adherent = new Adherent();
        em.persist(adherent);
        Adhesion adhesion = new Adhesion();
        adhesion.setAdherent(adherent);
        adhesion.setActivite(activite);
        adhesion.setStatutActuel(statut);
        em.persist(adhesion);
        return adherent;
    }
}
