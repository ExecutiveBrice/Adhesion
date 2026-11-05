package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

class TeamManagementMigrationTest {
    @Test void migrationPreservesExistingActivitiesSessionsAndAttendanceAndCanBeRerun() throws Exception {
        try (var ds = new SingleConnectionDataSource("jdbc:h2:mem:teams_migration;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            for (String table : new String[]{"activites", "adherents", "adhesions", "planifications_hebdomadaires", "seance"}) {
                jdbc.execute("CREATE TABLE " + table + " (id bigint primary key)");
                jdbc.execute("INSERT INTO " + table + " VALUES (7)");
            }
            jdbc.execute("CREATE TABLE presence (id bigint primary key, presence boolean)");
            jdbc.execute("INSERT INTO presence VALUES (1, true)");
            SpringLiquibase migration = new SpringLiquibase(); migration.setDataSource(ds);
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v16.xml"); migration.afterPropertiesSet(); migration.afterPropertiesSet();
            assertThat(jdbc.queryForObject("SELECT team_management FROM activites WHERE id=7", Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT team_managed FROM seance WHERE id=7", Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT presence FROM presence WHERE id=1", Boolean.class)).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM equipes", Integer.class)).isZero();
            jdbc.execute("INSERT INTO equipes (activite_id, nom) VALUES (7, 'A')");
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO equipes (activite_id, nom) VALUES (7, 'A')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.execute("INSERT INTO equipes (activite_id, nom) VALUES (999, 'B')"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }
}
