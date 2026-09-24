package com.wild.corp.adhesion.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("entityManagerFactory")
@Slf4j
public class RoleSchemaConstraintInitializer {

    private static final String SHOP_MANAGER_ROLE = "ROLE_RESPONSABLE_BOUTIQUE";

    private final JdbcTemplate jdbcTemplate;

    public RoleSchemaConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureRoleConstraintIsCurrent() {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT to_regclass('user_role_names') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(tableExists)) {
            log.warn("Table user_role_names absente : contrôle de la contrainte des rôles ignoré");
            return;
        }

        Boolean roleAlreadyAllowed = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conrelid = 'user_role_names'::regclass
                      AND contype = 'c'
                      AND pg_get_constraintdef(oid) LIKE ?
                )
                """, Boolean.class, "%" + SHOP_MANAGER_ROLE + "%");
        if (Boolean.TRUE.equals(roleAlreadyAllowed)) {
            log.info("Contrainte des rôles vérifiée : {} est autorisé", SHOP_MANAGER_ROLE);
            return;
        }

        jdbcTemplate.execute("""
                DO $$
                DECLARE
                    role_check record;
                BEGIN
                    FOR role_check IN
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = 'user_role_names'::regclass
                          AND contype = 'c'
                          AND pg_get_constraintdef(oid) LIKE '%role_name%'
                    LOOP
                        EXECUTE format('ALTER TABLE user_role_names DROP CONSTRAINT %I', role_check.conname);
                    END LOOP;

                    ALTER TABLE user_role_names ADD CONSTRAINT user_role_names_role_name_check
                        CHECK (role_name IN (
                            'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
                            'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
                            'ROLE_RESPONSABLE_BOUTIQUE'
                        ));
                END $$;
                """);
        log.info("Contrainte des rôles mise à jour avec {}", SHOP_MANAGER_ROLE);
    }
}
