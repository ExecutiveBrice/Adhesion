package com.wild.corp.adhesion.repository;

import com.wild.corp.adhesion.models.Historique;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ContextConfiguration(classes = NewsletterHistoryRepositoryTest.JpaApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NewsletterHistoryRepositoryTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Historique.class)
    @EnableJpaRepositories(basePackageClasses = HistoriqueRepository.class)
    static class JpaApplication { }
    @Autowired HistoriqueRepository history;

    @Test
    void durableCampaignClaimIsUniqueAndLegacyMailStillAllowsSeveralEntries() {
        Historique first = entry(); first.setCampaignId(7L); first.setCampaignStatus("UNKNOWN");
        history.saveAndFlush(first);
        Historique duplicate = entry(); duplicate.setCampaignId(7L);
        assertThatThrownBy(() -> history.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(history.findByCampaignId(7L).orElseThrow().getCampaignStatus()).isEqualTo("UNKNOWN");
        history.saveAndFlush(entry()); history.saveAndFlush(entry());
        assertThat(history.count()).isEqualTo(3);
        history.deleteAll();
    }
    private Historique entry() { return new Historique("Newsletter ALOD", "Objet", null, null, LocalDateTime.now()); }
}
