package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ChatMigrationTest {
    @Test
    void removesFormerReferentRolesAndTheirChatPermissions() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:referent_roles;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE user_role_names (user_id bigint, role_name varchar(32))");
            jdbc.execute("CREATE TABLE chat_permissions (chat_id bigint, role varchar(32))");
            jdbc.execute("INSERT INTO user_role_names VALUES (1, 'ROLE_REFERENT_ACTIVITE'), (1, 'ROLE_USER'), (2, 'ROLE_REFERENT_SECTION'), (3, 'ROLE_COMMUNICATION_SECTION')");
            jdbc.execute("INSERT INTO chat_permissions VALUES (1, 'ROLE_REFERENT_SECTION'), (1, 'ROLE_USER'), (2, 'ROLE_COMMUNICATION_SECTION')");
            migrate(dataSource, "v2/changeset-v12.xml");
            migrate(dataSource, "v2/changeset-v13.xml");

            assertThat(jdbc.queryForList("SELECT role_name FROM user_role_names", String.class)).containsExactly("ROLE_USER");
            assertThat(jdbc.queryForList("SELECT role FROM chat_permissions", String.class)).containsExactly("ROLE_USER");
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO user_role_names VALUES (2, 'ROLE_REFERENT_ACTIVITE')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO user_role_names VALUES (3, 'ROLE_COMMUNICATION_SECTION')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }

    @Test
    void addsConstraintsWithoutLosingChatsMessagesRolesOrReadCursors() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:unified_chats;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE activites (id bigint PRIMARY KEY)");
            jdbc.execute("CREATE TABLE sections (id bigint PRIMARY KEY)");
            jdbc.execute("CREATE TABLE users (id bigint PRIMARY KEY)");
            migrate(dataSource, "v2/chat.xml");
            jdbc.execute("INSERT INTO activites VALUES (7)");
            jdbc.execute("INSERT INTO sections VALUES (20)");
            jdbc.execute("INSERT INTO users VALUES (4), (5)");
            jdbc.execute("INSERT INTO chats (id, nom, cible, section_id, activite_id) VALUES (8, 'Section', 'SECTION', 20, NULL), (9, 'Activité', 'ACTIVITE', NULL, 7), (10, 'Global', 'ASSOCIATION', NULL, NULL)");
            jdbc.execute("INSERT INTO chat_permissions (chat_id, role, ecriture) VALUES (8, 'ROLE_USER', FALSE), (10, 'ROLE_SECRETAIRE', TRUE)");
            jdbc.execute("INSERT INTO chat_messages (id, chat_id, sender_user_id, sender_name, sender_role, content, created_at) VALUES (11, 8, 5, 'Alice', 'ENCADRANT', 'Ancien', CURRENT_TIMESTAMP), (12, 8, 4, 'Bob', NULL, 'Moi', CURRENT_TIMESTAMP), (13, 8, 5, 'Alice', NULL, 'Nouveau', CURRENT_TIMESTAMP), (14, 9, 5, 'Alice', NULL, 'Autre chat', CURRENT_TIMESTAMP)");
            jdbc.execute("INSERT INTO chat_reads VALUES (4, 8, 11)");
            migrate(dataSource, "v2/changeset-v10.xml");

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chats", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_permissions", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT sender_role FROM chat_messages WHERE id = 11", String.class)).isEqualTo("ENCADRANT");
            assertThat(jdbc.queryForObject("SELECT last_message_id FROM chat_reads WHERE user_id = 4 AND chat_id = 8", Long.class)).isEqualTo(11L);
            ChatReadRepository reads = new ChatReadRepository(jdbc);
            assertThat(reads.unreadCounts(4L, List.of(8L, 9L))).containsEntry(8L, 1L).containsEntry(9L, 1L);
            assertThat(reads.unreadCounts(5L, List.of(8L))).containsEntry(8L, 1L);
            assertThat(reads.unreadCounts(4L, List.of())).isEmpty();

            assertThatThrownBy(() -> jdbc.execute("INSERT INTO chat_messages (chat_id, sender_user_id, sender_name, content, created_at) VALUES (NULL, 4, 'Bob', 'Bonjour', CURRENT_TIMESTAMP)"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO chats (nom, cible, section_id, activite_id) VALUES ('Invalide', 'SECTION', 20, 7)"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO chats (nom, cible, section_id) VALUES ('Invalide', 'ASSOCIATION', 20)"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO chat_messages (chat_id, sender_user_id, sender_name, content, created_at) VALUES (8, 4, 'Bob', '  ', CURRENT_TIMESTAMP)"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.execute("INSERT INTO section_referents VALUES (20, 4)");
            jdbc.execute("DELETE FROM users WHERE id = 4");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_reads", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM section_referents", Integer.class)).isZero();
            jdbc.execute("DELETE FROM chats WHERE id = 8");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE chat_id = 8", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_permissions WHERE chat_id = 8", Integer.class)).isZero();
            jdbc.execute("DELETE FROM activites WHERE id = 7");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chats WHERE id = 10", Integer.class)).isEqualTo(1);
        }
    }

    private void migrate(SingleConnectionDataSource dataSource, String file) throws Exception {
        SpringLiquibase migration = new SpringLiquibase();
        migration.setDataSource(dataSource);
        migration.setChangeLog("classpath:db/changelog/" + file);
        migration.afterPropertiesSet();
    }
}
