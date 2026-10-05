package com.wild.corp.adhesion.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Repository
public class PubliciteRepository {
    public record Publicite(Long id, String description, String facebookUrl, String snapchatUrl, String websiteUrl,
                            List<Long> sectionIds, int displayOrder, boolean active) {}
    public record Image(String contentType, byte[] bytes) {}

    private final JdbcTemplate jdbc;

    public PubliciteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Publicite> findAll() {
        return withSections(jdbc.query("SELECT id, description, facebook_url, snapchat_url, website_url, display_order, active FROM publicites ORDER BY display_order, id DESC",
                publiciteRowMapper()));
    }

    public Optional<Publicite> findById(long id) {
        return withSections(jdbc.query("SELECT id, description, facebook_url, snapchat_url, website_url, display_order, active FROM publicites WHERE id = ?",
                publiciteRowMapper(), id)).stream().findFirst();
    }

    public List<Publicite> findVisible(String username) {
        return withSections(jdbc.query("""
                SELECT p.id, p.description, p.facebook_url, p.snapchat_url, p.website_url, p.display_order, p.active
                FROM publicites p
                WHERE p.active = TRUE AND (
                   NOT EXISTS (SELECT 1 FROM publicite_sections ps WHERE ps.publicite_id = p.id)
                   OR EXISTS (
                     SELECT 1 FROM publicite_sections ps
                     JOIN activites a ON a.section_id = ps.section_id
                     JOIN adhesions h ON (h.activite_id = a.id OR h.sur_classement_id = a.id)
                     JOIN adherents d ON d.id = h.adherent_id
                     JOIN users u ON u.id = d.user_id
                     WHERE ps.publicite_id = p.id AND u.username = ?
                       AND (h.statut_actuel IS NULL OR h.statut_actuel NOT IN ('Sur liste d''attente', 'Annulée'))
                   ))
                ORDER BY p.display_order, p.id DESC
                """, publiciteRowMapper(), username));
    }

    public Optional<Image> findImage(long id, String username, boolean viewAll) {
        String visibility = viewAll ? "" : """
                 AND p.active = TRUE
                 AND (NOT EXISTS (SELECT 1 FROM publicite_sections ps WHERE ps.publicite_id = p.id)
                   OR EXISTS (
                     SELECT 1 FROM publicite_sections ps
                     JOIN activites a ON a.section_id = ps.section_id
                     JOIN adhesions h ON (h.activite_id = a.id OR h.sur_classement_id = a.id)
                     JOIN adherents d ON d.id = h.adherent_id
                     JOIN users u ON u.id = d.user_id
                     WHERE ps.publicite_id = p.id AND u.username = ?
                       AND (h.statut_actuel IS NULL OR h.statut_actuel NOT IN ('Sur liste d''attente', 'Annulée'))
                   ))
                """;
        Object[] parameters = viewAll ? new Object[]{id} : new Object[]{id, username};
        return jdbc.query("SELECT p.content_type, p.image FROM publicites p WHERE p.id = ?" + visibility,
                (rs, row) -> new Image(rs.getString("content_type"), rs.getBytes("image")), parameters)
                .stream().findFirst();
    }

    @Transactional
    public Publicite create(String description, String facebookUrl, String snapchatUrl, String websiteUrl,
                            List<Long> sectionIds, int displayOrder, boolean active, String contentType, byte[] image) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO publicites (description, facebook_url, snapchat_url, website_url, display_order, active, content_type, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"});
            statement.setString(1, description);
            statement.setString(2, facebookUrl);
            statement.setString(3, snapchatUrl);
            statement.setString(4, websiteUrl);
            statement.setInt(5, displayOrder);
            statement.setBoolean(6, active);
            statement.setString(7, contentType);
            statement.setBytes(8, image);
            return statement;
        }, key);
        long id = key.getKey().longValue();
        if (!sectionIds.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO publicite_sections (publicite_id, section_id) VALUES (?, ?)", sectionIds,
                    sectionIds.size(), (statement, sectionId) -> {
                        statement.setLong(1, id);
                        statement.setLong(2, sectionId);
                    });
        }
        return new Publicite(id, description, facebookUrl, snapchatUrl, websiteUrl, List.copyOf(sectionIds),
                displayOrder, active);
    }

    public boolean updateSettings(long id, int displayOrder, boolean active) {
        return jdbc.update("UPDATE publicites SET display_order = ?, active = ? WHERE id = ?",
                displayOrder, active, id) > 0;
    }

    @Transactional
    public Optional<Publicite> update(long id, String description, String facebookUrl, String snapchatUrl,
                                      String websiteUrl, List<Long> sectionIds, int displayOrder, boolean active,
                                      String contentType, byte[] image) {
        int changed = image == null
                ? jdbc.update("""
                        UPDATE publicites SET description = ?, facebook_url = ?, snapchat_url = ?, website_url = ?,
                            display_order = ?, active = ? WHERE id = ?
                        """, description, facebookUrl, snapchatUrl, websiteUrl, displayOrder, active, id)
                : jdbc.update("""
                        UPDATE publicites SET description = ?, facebook_url = ?, snapchat_url = ?, website_url = ?,
                            display_order = ?, active = ?, content_type = ?, image = ? WHERE id = ?
                        """, description, facebookUrl, snapchatUrl, websiteUrl, displayOrder, active,
                        contentType, image, id);
        if (changed == 0) return Optional.empty();
        jdbc.update("DELETE FROM publicite_sections WHERE publicite_id = ?", id);
        if (!sectionIds.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO publicite_sections (publicite_id, section_id) VALUES (?, ?)", sectionIds,
                    sectionIds.size(), (statement, sectionId) -> {
                        statement.setLong(1, id);
                        statement.setLong(2, sectionId);
                    });
        }
        return findById(id);
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM publicites WHERE id = ?", id) > 0;
    }

    private List<Publicite> withSections(List<Publicite> publicites) {
        if (publicites.isEmpty()) return publicites;
        var sections = new HashMap<Long, List<Long>>();
        jdbc.query("SELECT publicite_id, section_id FROM publicite_sections ORDER BY section_id", rows -> {
            sections.computeIfAbsent(rows.getLong("publicite_id"), ignored -> new ArrayList<>())
                    .add(rows.getLong("section_id"));
        });
        return publicites.stream().map(item -> new Publicite(item.id(), item.description(), item.facebookUrl(),
                item.snapchatUrl(), item.websiteUrl(), List.copyOf(sections.getOrDefault(item.id(), List.of())),
                item.displayOrder(), item.active())).toList();
    }

    private static RowMapper<Publicite> publiciteRowMapper() {
        return (rows, row) -> new Publicite(rows.getLong("id"), rows.getString("description"),
                rows.getString("facebook_url"), rows.getString("snapchat_url"), rows.getString("website_url"),
                List.of(), rows.getInt("display_order"), rows.getBoolean("active"));
    }
}
