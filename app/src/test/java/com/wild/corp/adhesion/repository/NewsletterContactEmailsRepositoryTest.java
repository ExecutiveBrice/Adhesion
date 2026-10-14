package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ContextConfiguration(classes = NewsletterHistoryRepositoryTest.JpaApplication.class)
class NewsletterContactEmailsRepositoryTest {
    @Autowired AdherentRepository adherents;

    @Test
    void usesChosenRepresentativeAndIncludesMembersWithoutAnAccount() {
        Adherent parent = member("parent@example.org");
        Adherent child = member("child@example.org");
        child.setEmailRepresentant(true); child.setRepresentant(parent);
        adherents.saveAndFlush(child);
        Adherent ownEmail = member("own@example.org");
        ownEmail.setEmailRepresentant(false); ownEmail.setRepresentant(parent);
        adherents.saveAndFlush(ownEmail);
        adherents.saveAndFlush(new Adherent());
        assertThat(adherents.findAllContactEmails())
                .containsExactly("parent@example.org", "parent@example.org", "own@example.org", null);
    }

    private Adherent member(String email) {
        Adherent member = new Adherent();
        member.setUser(new User(email, "password"));
        return adherents.saveAndFlush(member);
    }
}
