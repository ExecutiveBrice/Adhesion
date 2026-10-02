package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.utils.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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
    void onlyIncludesValidMembershipsSurclassementAndOwnTeachingOrReferentGroups() {
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
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.VALIDEE.label)))
                .extracting(Activite::getId).containsExactlyInAnyOrder(registered.getId(), upper.getId(), teacher.getId(), referent.getId());
        valid = em.find(Adhesion.class, valid.getId());
        valid.setStatutActuel(Status.ANNULEE.label);
        em.flush(); em.clear();
        assertThat(activities.findChatActivities(user.getId(), List.of(Status.VALIDEE.label)))
                .extracting(Activite::getId).containsExactlyInAnyOrder(teacher.getId(), referent.getId());
    }

    @Test
    void boundsHistoryAndKeepsMessagesIsolatedByActivity() {
        Activite room = activity("Pilates");
        Activite other = activity("Yoga");
        for (int i = 0; i < 61; i++) {
            ChatMessage message = new ChatMessage();
            message.setActivite(i == 60 ? other : room);
            message.setChannel(ChatChannel.ACTIVITY);
            message.setSenderUserId(1L); message.setSenderName("Alice");
            message.setContent("Message " + i); message.setCreatedAt(Instant.now());
            em.persist(message);
        }
        em.flush(); em.clear();
        var latest = messages.findByActiviteIdOrderByIdDesc(room.getId(), PageRequest.of(0, 50));
        assertThat(latest).hasSize(50).extracting(ChatMessage::getContent).doesNotContain("Message 60");
        var oldest = latest.getLast().getId();
        assertThat(messages.findByActiviteIdAndIdLessThanOrderByIdDesc(room.getId(), oldest, PageRequest.of(0, 50)))
                .hasSize(10);
        assertThat(messages.findByActiviteIdAndIdGreaterThanOrderByIdAsc(room.getId(), latest.getFirst().getId(), PageRequest.of(0, 50)))
                .isEmpty();
    }

    @Test
    void communicationHistoryIsIsolatedAndSupportsBothPaginationDirections() {
        Activite room = activity("Pilates");
        ChatMessage first = null;
        ChatMessage last = null;
        for (int i = 0; i < 3; i++) {
            ChatMessage message = new ChatMessage();
            message.setActivite(i == 1 ? room : null);
            message.setChannel(i == 1 ? ChatChannel.ACTIVITY : ChatChannel.COMMUNICATION);
            message.setSenderUserId(1L); message.setSenderName("Alice");
            message.setContent("Message " + i); message.setCreatedAt(Instant.now());
            em.persist(message);
            if (i == 0) first = message;
            if (i == 2) last = message;
        }
        em.flush(); em.clear();
        var page = PageRequest.of(0, 50);
        assertThat(messages.findByChannelOrderByIdDesc(ChatChannel.COMMUNICATION, page))
                .extracting(ChatMessage::getId).containsExactly(last.getId(), first.getId());
        assertThat(messages.findByChannelAndIdLessThanOrderByIdDesc(ChatChannel.COMMUNICATION, last.getId(), page))
                .extracting(ChatMessage::getId).containsExactly(first.getId());
        assertThat(messages.findByChannelAndIdGreaterThanOrderByIdAsc(ChatChannel.COMMUNICATION, first.getId(), page))
                .extracting(ChatMessage::getId).containsExactly(last.getId());
        assertThat(messages.findByActiviteIdOrderByIdDesc(room.getId(), page)).hasSize(1);
    }

    private Activite activity(String name) {
        Activite activity = new Activite(); activity.setNom(name); em.persist(activity); return activity;
    }

    private Adhesion membership(Adherent member, Activite activity, Status status) {
        Adhesion adhesion = new Adhesion(); adhesion.setAdherent(member); adhesion.setActivite(activity);
        adhesion.setStatutActuel(status.label); em.persist(adhesion); return adhesion;
    }
}
