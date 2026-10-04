package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = AdherentRoleRepositoryTest.JpaTestApplication.class)
class ChatRepositoryTest {
    @Autowired private EntityManager em;
    @Autowired private ActiviteRepository activities;
    @Autowired private ChatMessageRepository messages;

    @Test
    void excludesCancelledAndWaitingMembershipsAndKeepsOwnTeachingOrReferentGroups() {
        User user = new User("alice@example.test", "password");
        Adherent member = new Adherent(); member.setUser(user); em.persist(member);
        Activite registered = activity("Pilates");
        Activite upper = activity("Surclassement");
        Activite teacher = activity("Cours");
        Activite referent = activity("Référent");
        Activite cancelled = activity("Annulée");
        Activite waiting = activity("Attente");
        activity("Autre groupe");
        Adhesion valid = membership(member, registered, Status.VALIDEE);
        valid.setSurClassement(upper);
        membership(member, cancelled, Status.ANNULEE);
        membership(member, waiting, Status.LISTE_ATTENTE);
        member.getCours().add(teacher);
        member.getActivitesReferent().add(referent);
        em.flush(); em.clear();
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label)))
                .extracting(Activite::getId).containsExactlyInAnyOrder(registered.getId(), upper.getId(), teacher.getId(), referent.getId());
        valid = em.find(Adhesion.class, valid.getId());
        valid.setStatutActuel(Status.ANNULEE.label);
        em.flush(); em.clear();
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label)))
                .extracting(Activite::getId).containsExactlyInAnyOrder(teacher.getId(), referent.getId());
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void chatMembershipIncludesEveryStatusExceptWaitingListAndCancelled(Status status) {
        User user = new User("member@example.test", "password");
        Adherent member = new Adherent();
        member.setUser(user);
        em.persist(member);
        Activite registered = activity("Activité");
        Activite upper = activity("Surclassement");
        Adhesion adhesion = membership(member, registered, status);
        adhesion.setSurClassement(upper);
        em.flush();
        em.clear();

        var accessible = activities.findChatActivities(user.getId(), List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label));
        if (status == Status.LISTE_ATTENTE || status == Status.ANNULEE) {
            assertThat(accessible).isEmpty();
        } else {
            assertThat(accessible).extracting(Activite::getId).containsExactlyInAnyOrder(registered.getId(), upper.getId());
        }
    }

    @Test
    void boundsHistoryAndKeepsMessagesIsolatedByChatEvenForTheSameActivity() {
        Activite activity = activity("Pilates");
        Chat room = chat("Pilates", activity);
        Chat other = chat("Informations Pilates", activity);
        for (int i = 0; i < 61; i++) {
            ChatMessage message = new ChatMessage();
            message.setChat(i == 60 ? other : room);
            message.setSenderUserId(1L); message.setSenderName("Alice");
            message.setContent("Message " + i); message.setCreatedAt(Instant.now());
            em.persist(message);
        }
        em.flush(); em.clear();
        var latest = messages.findByChatIdOrderByIdDesc(room.getId(), PageRequest.of(0, 50));
        assertThat(latest).hasSize(50).extracting(ChatMessage::getContent).doesNotContain("Message 60");
        var oldest = latest.getLast().getId();
        assertThat(messages.findByChatIdAndIdLessThanOrderByIdDesc(room.getId(), oldest, PageRequest.of(0, 50)))
                .hasSize(10);
        assertThat(messages.findByChatIdAndIdGreaterThanOrderByIdAsc(room.getId(), latest.getFirst().getId(), PageRequest.of(0, 50)))
                .isEmpty();
    }

    @Test
    void sectionReferentsAccessOnlyTheirSectionsActivities() {
        User user = new User("section@example.test", "password"); em.persist(user);
        Section section = Section.builder().nom("Basket").type("COMPETITION").build();
        section.getReferents().add(user); em.persist(section);
        Activite linked = activity("Basket"); linked.setSection(section);
        activity("Autre section");
        em.flush(); em.clear();
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label)))
                .extracting(Activite::getId).containsExactly(linked.getId());
        em.find(Section.class, section.getId()).getReferents().clear();
        em.flush(); em.clear();
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.LISTE_ATTENTE.label, Status.ANNULEE.label))).isEmpty();
    }

    private Chat chat(String name, Activite activity) {
        Chat chat = new Chat(); chat.setNom(name); chat.setCible(ChatTarget.ACTIVITE); chat.setActivite(activity);
        em.persist(chat); return chat;
    }

    private Activite activity(String name) {
        Activite activity = new Activite(); activity.setNom(name); em.persist(activity); return activity;
    }

    private Adhesion membership(Adherent member, Activite activity, Status status) {
        Adhesion adhesion = new Adhesion(); adhesion.setAdherent(member); adhesion.setActivite(activity);
        adhesion.setStatutActuel(status.label); em.persist(adhesion); return adhesion;
    }
}
