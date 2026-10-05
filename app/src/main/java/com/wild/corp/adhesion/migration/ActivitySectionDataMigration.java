package com.wild.corp.adhesion.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copies the production activity grouping data into the V2 section model.
 * Schema changes stay in the XML changelog; this class only moves existing rows.
 */
public final class ActivitySectionDataMigration implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        var connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try {
            var activities = readActivities(connection);
            var sectionTypes = collectSectionTypes(activities);
            insertSections(connection, sectionTypes);
            var sectionIds = readSectionIds(connection);
            linkActivities(connection, activities, sectionIds);
        } catch (SQLException exception) {
            throw new CustomChangeException("Unable to migrate activities to sections", exception);
        }
    }

    private Map<Long, ActivityGroup> readActivities(Connection connection) throws SQLException {
        var activities = new LinkedHashMap<Long, ActivityGroup>();
        try (var statement = connection.prepareStatement(
                "SELECT id, groupe_filtre, groupe FROM activites ORDER BY id");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                activities.put(rows.getLong("id"), new ActivityGroup(
                        sectionName(rows.getString("groupe_filtre"), rows.getString("groupe")),
                        rows.getString("groupe")));
            }
        }
        return activities;
    }

    private Map<String, String> collectSectionTypes(Map<Long, ActivityGroup> activities) {
        var sectionTypes = new LinkedHashMap<String, String>();
        for (var activity : activities.values()) {
            if (activity.sectionName() == null) {
                continue;
            }
            sectionTypes.merge(activity.sectionName(), sectionType(activity.group()),
                    (current, candidate) -> "COMPETITION".equals(candidate) ? candidate : current);
        }
        return sectionTypes;
    }

    private void insertSections(Connection connection, Map<String, String> sectionTypes) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO sections (nom, type) VALUES (?, ?)")) {
            for (var section : sectionTypes.entrySet()) {
                statement.setString(1, section.getKey());
                statement.setString(2, section.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Map<String, Long> readSectionIds(Connection connection) throws SQLException {
        var sectionIds = new LinkedHashMap<String, Long>();
        try (var statement = connection.prepareStatement("SELECT id, nom FROM sections");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                sectionIds.put(rows.getString("nom"), rows.getLong("id"));
            }
        }
        return sectionIds;
    }

    private void linkActivities(Connection connection, Map<Long, ActivityGroup> activities,
                                Map<String, Long> sectionIds) throws SQLException {
        try (var statement = connection.prepareStatement(
                "UPDATE activites SET section_id = ? WHERE id = ?")) {
            for (var activity : activities.entrySet()) {
                var sectionId = sectionIds.get(activity.getValue().sectionName());
                if (sectionId == null) {
                    statement.setNull(1, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(1, sectionId);
                }
                statement.setLong(2, activity.getKey());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private String sectionName(String filteredGroup, String group) {
        if (filteredGroup != null && !filteredGroup.isBlank()) {
            return filteredGroup.trim();
        }
        return group == null || group.isBlank() ? null : group.trim();
    }

    private String sectionType(String group) {
        return "COMPETITION".equals(group) ? "COMPETITION" : "NON_COMPETITIVE";
    }

    @Override
    public String getConfirmationMessage() {
        return "Existing activities were linked to V2 sections";
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

    private record ActivityGroup(String sectionName, String group) {
    }
}
