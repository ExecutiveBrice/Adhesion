package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PubliciteRepositoryTest {
    @Test
    void migrationSupportsPublicationListingImagesAndDeletion() throws Exception {
        try (var source = new SingleConnectionDataSource("jdbc:h2:mem:publicites;MODE=PostgreSQL", "sa", "", true)) {
            var migration = new SpringLiquibase();
            migration.setDataSource(source);
            migration.setChangeLog("classpath:db/changelog/v2/communication.xml");
            migration.afterPropertiesSet();
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE sections (id bigint PRIMARY KEY, nom varchar(100), type varchar(20))");
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v3.xml");
            migration.afterPropertiesSet();
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v4.xml");
            migration.afterPropertiesSet();
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v5.xml");
            migration.afterPropertiesSet();
            jdbc.update("""
                    INSERT INTO publicites (id, description, content_type, image, snapchat_url, display_order, active)
                    VALUES (99, 'Lien existant', 'image/png', ?, 'https://instagram.com/alod', 0, TRUE)
                    """, new byte[]{1});
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v11.xml");
            migration.afterPropertiesSet();
            assertThat(jdbc.queryForObject("SELECT instagram_url FROM publicites WHERE id = 99", String.class))
                    .isEqualTo("https://instagram.com/alod");
            jdbc.update("DELETE FROM publicites WHERE id = 99");
            jdbc.execute("CREATE TABLE users (id bigint PRIMARY KEY, username varchar(255))");
            jdbc.execute("CREATE TABLE adherents (id bigint PRIMARY KEY, user_id bigint)");
            jdbc.execute("CREATE TABLE activites (id bigint PRIMARY KEY, section_id bigint)");
            jdbc.execute("CREATE TABLE adhesions (id bigint PRIMARY KEY, activite_id bigint, sur_classement_id bigint, adherent_id bigint, statut_actuel varchar(255))");
            jdbc.update("INSERT INTO sections (id, nom, type) VALUES (1, 'Basket', 'NON_COMPETITIVE'), (2, 'Danse', 'NON_COMPETITIVE')");
            jdbc.update("INSERT INTO users (id, username) VALUES (1, 'basket@alod.fr'), (2, 'danse@alod.fr'), (3, 'sans-section@alod.fr')");
            jdbc.update("INSERT INTO adherents (id, user_id) VALUES (1, 1), (2, 2), (3, 3)");
            jdbc.update("INSERT INTO activites (id, section_id) VALUES (1, 1), (2, 2)");
            jdbc.update("INSERT INTO adhesions (id, activite_id, adherent_id, statut_actuel) VALUES (1, 1, 1, 'Validée'), (2, 2, 2, 'Validée')");
            var repository = new PubliciteRepository(jdbc);
            assertThat(repository.findAll()).isEmpty();
            var first = repository.create("Première", "https://facebook.com/alod", null,
                    "https://alod.fr", List.of(), 20, true, "image/png", new byte[]{1, 2, 3});
            var second = repository.create("Deuxième", null, "https://instagram.com/alod",
                    null, List.of(1L), 10, true, "image/jpeg", new byte[]{4, 5});
            var third = repository.create("Troisième", null, null,
                    null, List.of(2L), 5, false, "image/jpeg", new byte[]{6});
            assertThat(repository.findAll()).containsExactly(third, second, first);
            assertThat(repository.findAll().get(2).websiteUrl()).isEqualTo("https://alod.fr");
            assertThat(repository.findVisible("basket@alod.fr")).containsExactly(second, first);
            assertThat(repository.findVisible("danse@alod.fr")).containsExactly(first);
            assertThat(repository.findVisible("sans-section@alod.fr")).containsExactly(first);
            var image = repository.findImage(first.id(), "sans-section@alod.fr", false).orElseThrow();
            assertThat(image.contentType()).isEqualTo("image/png");
            assertThat(image.bytes()).containsExactly(1, 2, 3);
            assertThat(repository.findImage(second.id(), "sans-section@alod.fr", false)).isEmpty();
            assertThat(repository.findImage(third.id(), "danse@alod.fr", false)).isEmpty();
            assertThat(repository.findImage(second.id(), "communication@alod.fr", true)).isPresent();
            assertThat(repository.updateSettings(third.id(), 2, true)).isTrue();
            var updatedThird = repository.findById(third.id()).orElseThrow();
            assertThat(updatedThird.active()).isTrue();
            assertThat(repository.findVisible("danse@alod.fr")).containsExactly(updatedThird, first);
            var updatedSecond = repository.update(second.id(), "Deuxième modifiée", null, null,
                    "https://alod.fr", List.of(2L), 4, false, null, null).orElseThrow();
            assertThat(updatedSecond.sectionIds()).containsExactly(2L);
            assertThat(updatedSecond.active()).isFalse();
            assertThat(repository.findImage(second.id(), "communication@alod.fr", true).orElseThrow().bytes())
                    .containsExactly(4, 5);
            repository.update(second.id(), "Deuxième modifiée", null, null, "https://alod.fr", List.of(2L),
                    4, true, "image/png", new byte[]{9});
            assertThat(repository.findImage(second.id(), "communication@alod.fr", true).orElseThrow().bytes())
                    .containsExactly(9);
            updatedSecond = repository.findById(second.id()).orElseThrow();
            assertThat(repository.delete(first.id())).isTrue();
            assertThat(repository.delete(first.id())).isFalse();
            assertThat(repository.findImage(first.id(), "sans-section@alod.fr", false)).isEmpty();
            assertThat(repository.findAll()).containsExactly(updatedThird, updatedSecond);
        }
    }
}
