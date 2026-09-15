package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleTableMigrationTest {
    private JdbcTemplate database() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:role_tables_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"));
        jdbc.execute("create table user_role_names (user_id bigint, role_name varchar(32), primary key (user_id, role_name))");
        jdbc.execute("create table roles (id integer primary key, name varchar(20))");
        jdbc.execute("create table users_roles (user_id bigint, roles_id integer, primary key (user_id, roles_id))");
        return jdbc;
    }

    @Test
    void transfersAssignmentsNormalizesOldNameAndRemovesOldTables() {
        JdbcTemplate jdbc = database();
        jdbc.update("insert into roles (id, name) values (1, 'ROLE_ADMIN'), (2, 'ROLE_ADMINISTRATEUR'), (3, 'ROLE_MEMBRECA')");
        jdbc.update("insert into users_roles (user_id, roles_id) values (42, 1), (42, 2), (42, 3), (43, 2)");
        jdbc.update("insert into user_role_names (user_id, role_name) values (42, 'ROLE_ADMIN')");
        RoleTableMigration migration = new RoleTableMigration(jdbc);

        migration.migrate();
        migration.migrate();

        assertEquals(3, jdbc.queryForObject("select count(*) from user_role_names", Integer.class));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from user_role_names where role_name = 'ROLE_MEMBRECA'", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'PUBLIC' and lower(table_name) in ('roles', 'users_roles')",
                Integer.class));
    }

    @Test
    void refusesUnknownRoleBeforeRemovingOldTables() {
        JdbcTemplate jdbc = database();
        jdbc.update("insert into roles (id, name) values (1, 'ROLE_INCONNU')");

        assertThrows(IllegalArgumentException.class, () -> new RoleTableMigration(jdbc).migrate());

        assertEquals(1, jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'PUBLIC' and lower(table_name) = 'roles'",
                Integer.class));
    }
}
