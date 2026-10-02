package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

class ChatMigrationTest {
    @Test
    void migratesAndPersistsMessagesWithActivityCascade() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:chat_migration;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE activites (id bigint PRIMARY KEY)");
            SpringLiquibase migration = new SpringLiquibase();
            migration.setDataSource(dataSource);
            migration.setChangeLog("classpath:db/changelog/chat/01-create_activity_chat.xml");
            migration.afterPropertiesSet();
            jdbc.execute("INSERT INTO activites (id) VALUES (7)");
            jdbc.execute("INSERT INTO activity_chat_messages (activite_id, sender_user_id, sender_name, content, created_at) VALUES (7, 4, 'Alice', 'Bonjour', CURRENT_TIMESTAMP)");
            assertThat(jdbc.queryForObject("SELECT content FROM activity_chat_messages", String.class)).isEqualTo("Bonjour");
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO activity_chat_messages (activite_id, sender_user_id, sender_name, content, created_at) VALUES (7, 4, 'Alice', '  ', CURRENT_TIMESTAMP)"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.execute("DELETE FROM activites WHERE id = 7");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_chat_messages", Integer.class)).isZero();
        }
    }

    @Test
    void persistsReadCursorsAndCountsOnlyOthersMessagesAfterCursor() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:chat_reads;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE activites (id bigint PRIMARY KEY)");
            jdbc.execute("CREATE TABLE users (id bigint PRIMARY KEY)");
            for (String file : new String[] {"01-create_activity_chat.xml", "02-create_activity_chat_reads.xml", "03-create_communication_chat.xml", "05-create_referent_encadrant_chat.xml"}) {
                SpringLiquibase migration = new SpringLiquibase();
                migration.setDataSource(dataSource);
                migration.setChangeLog("classpath:db/changelog/chat/" + file);
                migration.afterPropertiesSet();
            }
            jdbc.execute("INSERT INTO activites VALUES (7), (8)");
            jdbc.execute("INSERT INTO users VALUES (4), (5)");
            jdbc.execute("INSERT INTO activity_chat_messages (id, activite_id, channel, sender_user_id, sender_name, content, created_at) VALUES (10, 7, 'ACTIVITY', 5, 'Alice', 'Ancien', CURRENT_TIMESTAMP), (11, 7, 'ACTIVITY', 4, 'Bob', 'Moi', CURRENT_TIMESTAMP), (12, 7, 'ACTIVITY', 5, 'Alice', 'Nouveau', CURRENT_TIMESTAMP), (13, 8, 'ACTIVITY', 5, 'Alice', 'Autre salon', CURRENT_TIMESTAMP)");
            ChatReadRepository reads = new ChatReadRepository(jdbc);
            jdbc.execute("INSERT INTO activity_chat_messages (id, activite_id, channel, sender_user_id, sender_name, content, created_at) VALUES (14, NULL, 'COMMUNICATION', 5, 'Alice', 'Communication', CURRENT_TIMESTAMP), (15, NULL, 'COMMUNICATION', 4, 'Bob', 'Ma communication', CURRENT_TIMESTAMP), (16, NULL, 'REFERENT_ENCADRANT', 5, 'Alice', 'Équipe', CURRENT_TIMESTAMP)");
            assertThat(reads.communicationUnreadCount(4L)).isEqualTo(1L);
            jdbc.execute("INSERT INTO communication_chat_reads VALUES (4, 14)");
            assertThat(reads.communicationUnreadCount(4L)).isZero();
            assertThat(reads.communicationUnreadCount(5L)).isEqualTo(1L);
            assertThat(reads.referentEncadrantUnreadCount(4L)).isEqualTo(1L);
            reads.markReferentEncadrantRead(4L, 16L);
            assertThat(reads.referentEncadrantUnreadCount(4L)).isZero();
            assertThat(reads.unreadCounts(4L, java.util.List.of(7L, 8L))).containsEntry(7L, 2L).containsEntry(8L, 1L);
            jdbc.execute("INSERT INTO activity_chat_reads VALUES (4, 7, 10)");
            assertThat(reads.unreadCounts(4L, java.util.List.of(7L))).containsEntry(7L, 1L).doesNotContainKey(8L);
            assertThat(reads.unreadCounts(5L, java.util.List.of(7L))).containsEntry(7L, 1L);
            jdbc.execute("DELETE FROM activites WHERE id = 7");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_chat_reads", Integer.class)).isZero();
            jdbc.execute("INSERT INTO activity_chat_reads VALUES (4, 8, 13)");
            jdbc.execute("DELETE FROM users WHERE id = 4");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_chat_reads", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM communication_chat_reads", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM referent_encadrant_chat_reads", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_chat_messages WHERE activite_id IS NULL", Integer.class)).isEqualTo(3);
        }
    }

    @Test
    void addsAnOptionalRoleToPreserveTheChatAuthorDisplay() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:chat_sender_role;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE activites (id bigint PRIMARY KEY)");
            for (String file : new String[] {"01-create_activity_chat.xml", "04-add_sender_role.xml"}) {
                SpringLiquibase migration = new SpringLiquibase();
                migration.setDataSource(dataSource);
                migration.setChangeLog("classpath:db/changelog/chat/" + file);
                migration.afterPropertiesSet();
            }
            jdbc.execute("INSERT INTO activites VALUES (7)");
            jdbc.execute("INSERT INTO activity_chat_messages (activite_id, sender_user_id, sender_name, sender_role, content, created_at) VALUES (7, 4, 'Alice', 'REFERENT', 'Bonjour', CURRENT_TIMESTAMP)");
            assertThat(jdbc.queryForObject("SELECT sender_role FROM activity_chat_messages", String.class)).isEqualTo("REFERENT");
        }
    }
}
