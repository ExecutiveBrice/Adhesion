package com.wild.corp.adhesion.repository;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

class ActivityEventsMigrationTest {
    @Test void convertsCategoriesAndPreservesEventHistoryWhileAssigningTheDefaultRoster() throws Exception {
        try (var ds = new SingleConnectionDataSource("jdbc:h2:mem:activity_events;MODE=PostgreSQL", "sa", "", true)) {
            JdbcTemplate db = new JdbcTemplate(ds);
            db.execute("CREATE TABLE activites (id bigint primary key, jour smallint, horaire_debut time, duree bigint)");
            db.execute("CREATE TABLE adherents (id bigint primary key)");
            db.execute("CREATE TABLE adhesions (id bigint primary key, adherent_id bigint, activite_id bigint, sur_classement_id bigint)");
            db.execute("CREATE TABLE adherents_cours (cours_id bigint, profs_id bigint)");
            db.execute("CREATE TABLE adherent_referent_activites (activite_id bigint, adherent_id bigint)");
            db.execute("CREATE TABLE planifications_hebdomadaires (id bigint primary key, activite_id bigint, descriptif varchar(100))");
            db.execute("CREATE TABLE planification_professeurs (planification_id bigint, adherent_id bigint)");
            db.execute("CREATE TABLE planification_referents (planification_id bigint, adherent_id bigint)");
            db.execute("CREATE TABLE seance (id bigint primary key, activite_id bigint, planification_id bigint REFERENCES planifications_hebdomadaires(id), debut timestamp, commentaire varchar(255))");
            db.execute("CREATE TABLE presence (id bigint primary key, seance_id bigint, adhesion_id bigint, presence_prevue boolean, presence boolean, date_modification date)");
            db.update("INSERT INTO activites (id) VALUES (1),(2),(3)");
            db.update("INSERT INTO adherents VALUES (1),(2),(3),(4)");
            db.update("INSERT INTO adhesions VALUES (1,1,1,null),(2,2,1,null),(3,3,2,1),(4,4,1,null)");
            db.update("INSERT INTO planifications_hebdomadaires VALUES (10,1,'Loisir'),(11,1,'Compétition'),(12,2,'Existant')");
            db.update("INSERT INTO planification_professeurs VALUES (10,1),(12,3)");
            db.update("INSERT INTO planification_referents VALUES (11,2)");
            db.update("INSERT INTO adherents_cours VALUES (1,4)");
            db.update("INSERT INTO seance VALUES (100,1,10,'2026-10-01 18:00','Historique'),(101,1,11,'2027-01-01 18:00','À venir'),(102,1,null,'2027-02-01 18:00',null),(103,2,12,'2027-03-01 18:00',null)");
            db.update("INSERT INTO presence VALUES (1,100,1,true,false,'2026-10-01'),(2,101,2,false,null,'2026-10-02'),(3,102,4,null,null,null),(4,103,3,null,null,null)");
            SpringLiquibase migration = new SpringLiquibase(); migration.setDataSource(ds);
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v16.xml"); migration.afterPropertiesSet();
            db.update("INSERT INTO equipes (activite_id,nom) VALUES (2,'Z première'),(2,'A seconde')");
            long existing = db.queryForObject("SELECT id FROM equipes WHERE nom='Z première'", Long.class);
            db.update("INSERT INTO planification_equipes VALUES (12,?)", existing);
            db.update("INSERT INTO seance_equipes VALUES (103,?)", existing);
            db.update("INSERT INTO convocations_equipe (seance_id,adherent_id,adhesion_id,selectionnee) VALUES (103,3,3,true)");
            migration.setChangeLog("classpath:db/changelog/v2/changeset-v17.xml");
            migration.afterPropertiesSet(); migration.afterPropertiesSet();

            assertThat(db.queryForList("SELECT nom FROM equipes WHERE activite_id=1 ORDER BY id", String.class)).containsExactly("Loisir", "Compétition");
            long first = db.queryForObject("SELECT id FROM equipes WHERE activite_id=1 AND nom='Loisir'", Long.class);
            assertThat(db.queryForList("SELECT adhesion_id FROM affectations_equipe WHERE equipe_id=? ORDER BY adhesion_id", Long.class, first)).containsExactly(1L,2L,3L,4L);
            assertThat(db.queryForObject("SELECT count(*) FROM equipes", Integer.class)).isEqualTo(5);
            assertThat(db.queryForObject("SELECT equipe_id FROM seance_equipes WHERE seance_id=100", Long.class)).isEqualTo(first);
            assertThat(db.queryForObject("SELECT equipe_id FROM seance_equipes WHERE seance_id=103", Long.class)).isEqualTo(existing);
            assertThat(db.queryForList("SELECT adherent_id FROM equipe_encadrants WHERE equipe_id=? ORDER BY adherent_id", Long.class, first)).containsExactly(1L,4L);
            assertThat(db.queryForObject("SELECT count(*) FROM presence", Integer.class)).isEqualTo(4);
            assertThat(db.queryForObject("SELECT presence FROM presence WHERE id=1", Boolean.class)).isFalse();
            assertThat(db.queryForObject("SELECT reponse FROM convocations_equipe WHERE seance_id=100", Boolean.class)).isTrue();
            assertThat(db.queryForObject("SELECT reponse FROM convocations_equipe WHERE seance_id=101", Boolean.class)).isFalse();
            assertThat(db.queryForObject("SELECT publication FROM convocations_equipe WHERE seance_id=102", java.sql.Timestamp.class)).isNotNull();
            assertThat(db.queryForObject("SELECT publication FROM convocations_equipe WHERE seance_id=103", java.sql.Timestamp.class)).isNull();
            assertThat(db.queryForObject("SELECT count(*) FROM convocations_equipe", Integer.class)).isEqualTo(4);
            assertThat(db.queryForObject("SELECT count(*) FROM team_mails", Integer.class)).isZero();
            assertThat(db.queryForList("SELECT type_evenement FROM seance", String.class)).containsOnly("ACTIVITE");
            assertThat(db.queryForObject("SELECT count(*) FROM information_schema.tables WHERE lower(table_name) LIKE 'planification%'", Integer.class)).isZero();
            assertThat(db.queryForObject("SELECT count(*) FROM information_schema.columns WHERE lower(column_name) IN ('team_managed','team_management','planification_id')", Integer.class)).isZero();
            db.update("UPDATE seance SET commentaire=? WHERE id=100", "x".repeat(2000));
            db.update("DELETE FROM presence WHERE adhesion_id=1");
            db.update("DELETE FROM adhesions WHERE id=1");
            assertThat(db.queryForObject("SELECT count(*) FROM affectations_equipe WHERE adhesion_id=1", Integer.class)).isZero();
            assertThat(db.queryForObject("SELECT count(*) FROM convocations_equipe WHERE adhesion_id=1", Integer.class)).isZero();
        }
    }
}
