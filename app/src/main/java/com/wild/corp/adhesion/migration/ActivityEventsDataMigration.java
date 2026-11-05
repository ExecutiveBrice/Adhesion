package com.wild.corp.adhesion.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import liquibase.resource.ResourceAccessor;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Moves the legacy planning data into teams and events before its schema is removed. */
public class ActivityEventsDataMigration implements CustomTaskChange {
    @Override public void execute(Database database) throws CustomChangeException {
        Connection c = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try {
            for (long activity : longs(c, "SELECT id FROM activites ORDER BY id")) migrateActivity(c, activity);
            update(c, "UPDATE seance SET type_evenement = 'ACTIVITE' WHERE type_evenement = 'SEANCE' OR type_evenement IS NULL");
            dropColumnReferences(c, "seance", "planification_id");
        } catch (SQLException error) { throw new CustomChangeException("Impossible de reprendre les activités et événements", error); }
    }
    private void migrateActivity(Connection c, long activity) throws SQLException {
        // A former category becomes a team; only its recurrence is discarded.
        // Existing explicitly linked teams keep their identifiers and roster.
        for (long plan : longs(c, "SELECT id FROM planifications_hebdomadaires WHERE activite_id = ? ORDER BY id", activity)) {
            if (exists(c, "SELECT equipe_id FROM planification_equipes WHERE planification_id = ?", plan)) continue;
            String label;
            try (PreparedStatement p = c.prepareStatement("SELECT descriptif FROM planifications_hebdomadaires WHERE id = ?")) {
                p.setLong(1, plan); try (ResultSet r = p.executeQuery()) { r.next(); label = r.getString(1); }
            }
            long team = createTeam(c, activity, label == null || label.isBlank() ? "Équipe principale" : label.trim());
            update(c, "INSERT INTO planification_equipes (planification_id, equipe_id) VALUES (?, ?)", plan, team);
        }
        List<Long> active = longs(c, "SELECT id FROM equipes WHERE activite_id = ? AND archivee = false ORDER BY id", activity);
        long first;
        if (active.isEmpty()) {
            first = createTeam(c, activity, "Équipe principale");
        } else first = active.getFirst();
        for (long person : longs(c, "SELECT profs_id FROM adherents_cours WHERE cours_id = ?", activity)) staff(c, "equipe_encadrants", first, person);
        for (long person : longs(c, "SELECT adherent_id FROM adherent_referent_activites WHERE activite_id = ?", activity)) staff(c, "equipe_referents", first, person);

        // All registrations are associated by default. Their existing status still controls participation.
        try (PreparedStatement p = c.prepareStatement("SELECT id, adherent_id FROM adhesions WHERE activite_id = ? OR sur_classement_id = ? ORDER BY id")) {
            p.setLong(1, activity); p.setLong(2, activity);
            try (ResultSet r = p.executeQuery()) {
                Set<Long> seen = new HashSet<>();
                while (r.next()) {
                    long membership = r.getLong(1), person = r.getLong(2);
                    if (seen.add(person) && !exists(c, "SELECT a.id FROM affectations_equipe a JOIN adhesions h ON h.id = a.adhesion_id WHERE a.equipe_id = ? AND h.adherent_id = ? AND a.fin IS NULL", first, person))
                        update(c, "INSERT INTO affectations_equipe (equipe_id, adhesion_id, debut) VALUES (?, ?, ?)", first, membership, java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Europe/Paris"))));
                }
            }
        }
        // Copy staff from each former category, even if it had no generated occurrence.
        for (long plan : longs(c, "SELECT id FROM planifications_hebdomadaires WHERE activite_id = ?", activity)) {
            List<Long> linked = longs(c, "SELECT p.equipe_id FROM planification_equipes p JOIN equipes e ON e.id = p.equipe_id WHERE p.planification_id = ? AND e.activite_id = ?", plan, activity);
            if (linked.isEmpty()) linked = List.of(first);
            for (long team : linked) {
                for (long person : longs(c, "SELECT adherent_id FROM planification_professeurs WHERE planification_id = ?", plan)) staff(c, "equipe_encadrants", team, person);
                for (long person : longs(c, "SELECT adherent_id FROM planification_referents WHERE planification_id = ?", plan)) staff(c, "equipe_referents", team, person);
            }
        }
        try (PreparedStatement p = c.prepareStatement("SELECT id, planification_id, debut FROM seance WHERE activite_id = ?")) {
            p.setLong(1, activity);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    long event = r.getLong(1); Long plan = r.getObject(2, Long.class); Timestamp start = r.getTimestamp(3);
                    if (!exists(c, "SELECT equipe_id FROM seance_equipes WHERE seance_id = ?", event)) {
                        List<Long> linked = plan == null ? List.of() : longs(c, "SELECT p.equipe_id FROM planification_equipes p JOIN equipes e ON e.id = p.equipe_id WHERE p.planification_id = ? AND e.activite_id = ?", plan, activity);
                        if (linked.isEmpty()) linked = List.of(first);
                        for (long team : linked) update(c, "INSERT INTO seance_equipes (seance_id, equipe_id) VALUES (?, ?)", event, team);
                    }
                    copyResponses(c, event, start);
                }
            }
        }
    }
    private void copyResponses(Connection c, long event, Timestamp start) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT p.adhesion_id, h.adherent_id, p.presence_prevue, p.date_modification FROM presence p JOIN adhesions h ON h.id = p.adhesion_id WHERE p.seance_id = ? ORDER BY p.id")) {
            p.setLong(1, event);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    long membership = r.getLong(1), person = r.getLong(2);
                    if (exists(c, "SELECT id FROM convocations_equipe WHERE seance_id = ? AND adherent_id = ?", event, person)) continue;
                    Boolean response = r.getObject(3, Boolean.class); java.sql.Date changed = r.getDate(4);
                    Timestamp date = changed == null ? start : Timestamp.valueOf(changed.toLocalDate().atStartOfDay());
                    update(c, "INSERT INTO convocations_equipe (seance_id, adherent_id, adhesion_id, selectionnee, publication, reponse, date_reponse) VALUES (?, ?, ?, true, ?, ?, ?)",
                            event, person, membership, date, response, response == null ? null : date);
                }
            }
        }
    }
    private long createTeam(Connection c, long activity, String label) throws SQLException {
        Set<String> names = new HashSet<>();
        try (PreparedStatement p = c.prepareStatement("SELECT nom FROM equipes WHERE activite_id = ?")) {
            p.setLong(1, activity); try (ResultSet r = p.executeQuery()) { while (r.next()) names.add(r.getString(1).toLowerCase(Locale.ROOT)); }
        }
        String name = label; int suffix = 2;
        while (names.contains(name.toLowerCase(Locale.ROOT))) {
            String ending = " " + suffix++;
            name = label.substring(0, Math.min(label.length(), 100 - ending.length())) + ending;
        }
        try (PreparedStatement p = c.prepareStatement("INSERT INTO equipes (activite_id, nom, archivee) VALUES (?, ?, false)", Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1, activity); p.setString(2, name); p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { if (!r.next()) throw new SQLException("Missing generated team identifier"); return r.getLong(1); }
        }
    }
    private void staff(Connection c, String table, long team, long person) throws SQLException {
        if (!exists(c, "SELECT adherent_id FROM " + table + " WHERE equipe_id = ? AND adherent_id = ?", team, person))
            update(c, "INSERT INTO " + table + " (equipe_id, adherent_id) VALUES (?, ?)", team, person);
    }
    private List<Long> longs(Connection c, String sql, Object... args) throws SQLException {
        List<Long> result = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, args); try (ResultSet r = p.executeQuery()) { while (r.next()) result.add(r.getLong(1)); } }
        return result;
    }
    private boolean exists(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, args); try (ResultSet r = p.executeQuery()) { return r.next(); } }
    }
    private void update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, args); p.executeUpdate(); }
    }
    private void bind(PreparedStatement p, Object[] args) throws SQLException { for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]); }
    private void dropColumnReferences(Connection c, String table, String column) throws SQLException {
        Set<String> constraints = new HashSet<>();
        for (String name : List.of(table, table.toUpperCase(Locale.ROOT))) {
            try (ResultSet r = c.getMetaData().getImportedKeys(c.getCatalog(), c.getSchema(), name)) {
                while (r.next()) if (column.equalsIgnoreCase(r.getString("FKCOLUMN_NAME"))) constraints.add(r.getString("FK_NAME"));
            }
        }
        for (String name : constraints) try (Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + table + " DROP CONSTRAINT \"" + name.replace("\"", "\"\"") + "\"");
        }
    }
    @Override public String getConfirmationMessage() { return "Activités reprises dans leurs équipes et événements"; }
    @Override public void setUp() { }
    @Override public void setFileOpener(ResourceAccessor accessor) { }
    @Override public ValidationErrors validate(Database database) { return new ValidationErrors(); }
}
