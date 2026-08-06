package com.wild.corp.adhesion.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate stores {@code ESeance} by ordinal in the existing schema. When a
 * new status is added, PostgreSQL's generated check constraint must therefore
 * be widened as well.
 */
@Component
public class SeanceSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SeanceSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE seance DROP CONSTRAINT IF EXISTS seance_etat_seance_check");
        jdbcTemplate.execute("""
                ALTER TABLE seance
                ADD CONSTRAINT seance_etat_seance_check CHECK (etat_seance >= 0 AND etat_seance <= 3)
                """);
    }
}
