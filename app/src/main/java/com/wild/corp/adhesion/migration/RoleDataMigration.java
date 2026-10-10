package com.wild.corp.adhesion.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Migrates historical roles whether or not production has a primary key on the role table. */
public final class RoleDataMigration implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        var connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try (var statement = connection.createStatement()) {
            var primaryKeyName = findPrimaryKeyName(connection);
            if (primaryKeyName != null) {
                statement.execute("ALTER TABLE user_role_names DROP CONSTRAINT " + quote(primaryKeyName));
            }
            statement.execute("ALTER TABLE user_role_names RENAME COLUMN role_name TO legacy_role_name");
            statement.execute("ALTER TABLE user_role_names ADD COLUMN role_name varchar(32)");
            statement.execute("""
                    UPDATE user_role_names
                    SET role_name = CASE
                        WHEN legacy_role_name = 'ROLE_REFERENT' THEN 'ROLE_REFERENT_ACTIVITE'
                        ELSE legacy_role_name
                    END
                    """);
            statement.execute("ALTER TABLE user_role_names ALTER COLUMN legacy_role_name DROP NOT NULL");
            statement.execute("ALTER TABLE user_role_names ALTER COLUMN role_name SET NOT NULL");
            statement.execute("""
                    ALTER TABLE user_role_names
                    ADD CONSTRAINT pk_user_role_names PRIMARY KEY (user_id, role_name)
                    """);
            statement.execute("""
                    ALTER TABLE user_role_names
                    ADD CONSTRAINT user_role_names_v2_role_name_check
                    CHECK (role_name IN (
                        'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
                        'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT',
                        'ROLE_REFERENT_ACTIVITE', 'ROLE_REFERENT_SECTION',
                        'ROLE_COMMUNICATION_SECTION', 'ROLE_COMMUNICATION_GLOBAL',
                        'ROLE_RESPONSABLE_BOUTIQUE'
                    ))
                    """);
        } catch (SQLException exception) {
            throw new CustomChangeException("Unable to migrate the V2 roles", exception);
        }
    }

    private String findPrimaryKeyName(Connection connection) throws SQLException {
        var metadata = connection.getMetaData();
        var schema = connection.getSchema();
        // A null schema scans every schema and can return another table's key
        // when the historical table in the current schema has none.
        String[] schemas = schema == null ? new String[]{null} : new String[]{schema, lower(schema), upper(schema)};
        String[] tables = {"user_role_names", "USER_ROLE_NAMES"};
        for (var schemaCandidate : schemas) {
            for (var tableCandidate : tables) {
                try (ResultSet keys = metadata.getPrimaryKeys(
                        connection.getCatalog(), schemaCandidate, tableCandidate)) {
                    if (keys.next()) {
                        return keys.getString("PK_NAME");
                    }
                }
            }
        }
        return null;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase(java.util.Locale.ROOT);
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(java.util.Locale.ROOT);
    }

    private String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    @Override
    public String getConfirmationMessage() {
        return "Historical roles migrated to V2";
    }

    @Override
    public void setUp() throws SetupException {
        // No setup required.
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // No external resource required.
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
