package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ERole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleTableMigration {
    private final JdbcTemplate jdbcTemplate;

    public RoleTableMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void migrate() {
        boolean rolesTableExists = tableExists("roles");
        boolean assignmentsTableExists = tableExists("users_roles");
        if (!rolesTableExists && !assignmentsTableExists) {
            return;
        }
        if (!rolesTableExists || !assignmentsTableExists) {
            throw new IllegalStateException("Anciennes tables de rôles incomplètes");
        }

        List<String> oldNames = jdbcTemplate.queryForList("select distinct name from roles", String.class);
        for (String oldName : oldNames) {
            ERole.valueOf(normalizedRoleName(oldName));
        }

        jdbcTemplate.update("""
                insert into user_role_names (user_id, role_name)
                select distinct old.user_id, old.role_name
                from (
                    select ur.user_id,
                           case when r.name = 'ROLE_ADMINISTRATEUR'
                                then 'ROLE_MEMBRECA' else r.name end as role_name
                    from users_roles ur join roles r on r.id = ur.roles_id
                ) old
                where not exists (
                    select 1 from user_role_names current_roles
                    where current_roles.user_id = old.user_id
                      and current_roles.role_name = old.role_name)
                """);

        Integer missing = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select distinct ur.user_id,
                           case when r.name = 'ROLE_ADMINISTRATEUR'
                                then 'ROLE_MEMBRECA' else r.name end as role_name
                    from users_roles ur join roles r on r.id = ur.roles_id
                ) old
                where not exists (
                    select 1 from user_role_names current_roles
                    where current_roles.user_id = old.user_id
                      and current_roles.role_name = old.role_name)
                """, Integer.class);
        if (missing == null || missing != 0) {
            throw new IllegalStateException("Des rôles utilisateurs n'ont pas été transférés");
        }

        jdbcTemplate.execute("drop table users_roles");
        jdbcTemplate.execute("drop table roles");
    }

    private String normalizedRoleName(String oldName) {
        return "ROLE_ADMINISTRATEUR".equals(oldName) ? "ROLE_MEMBRECA" : oldName;
    }

    private boolean tableExists(String tableName) {
        String schema = jdbcTemplate.execute((ConnectionCallback<String>) connection -> connection.getSchema());
        if (schema == null || schema.isBlank()) {
            throw new IllegalStateException("Schéma de base de données introuvable");
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ? and lower(table_schema) = lower(?)",
                Integer.class, tableName, schema);
        return count != null && count > 0;
    }
}
