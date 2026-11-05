package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

class ResponsabiliteEquipeMigrationTest {
    @Test void preservesHistoricalAssignmentsAndAddsAnOptionalTeamWithReferentialIntegrity() throws Exception {
        try (var ds = new SingleConnectionDataSource("jdbc:h2:mem:responsibility_teams;MODE=PostgreSQL", "sa", "", true)) {
            var db = new JdbcTemplate(ds);
            db.execute("CREATE TABLE equipes (id bigint primary key)");
            db.execute("INSERT INTO equipes VALUES (10)");
            db.execute("CREATE TABLE responsabilites_seance (seance_id bigint, position integer, tache_id bigint, adherent_id bigint)");
            db.execute("INSERT INTO responsabilites_seance VALUES (20,0,3,12)");
            var migration = new SpringLiquibase(); migration.setDataSource(ds);
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v18.xml");
            migration.afterPropertiesSet(); migration.afterPropertiesSet();
            assertThat(db.queryForObject("SELECT adherent_id FROM responsabilites_seance", Long.class)).isEqualTo(12L);
            assertThat(db.queryForObject("SELECT equipe_id FROM responsabilites_seance", Long.class)).isNull();
            db.update("UPDATE responsabilites_seance SET equipe_id=10");
            assertThatThrownBy(() -> db.update("UPDATE responsabilites_seance SET equipe_id=999"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }
}
