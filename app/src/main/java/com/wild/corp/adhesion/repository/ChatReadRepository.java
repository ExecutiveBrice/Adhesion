package com.wild.corp.adhesion.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ChatReadRepository {
    private final JdbcTemplate jdbc;

    public long communicationUnreadCount(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM activity_chat_messages m
                LEFT JOIN communication_chat_reads r ON r.user_id = ?
                WHERE m.channel = 'COMMUNICATION' AND m.sender_user_id <> ?
                AND m.id > COALESCE(r.last_message_id, 0)
                """, Long.class, userId, userId);
    }

    public long referentEncadrantUnreadCount(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM activity_chat_messages m
                LEFT JOIN referent_encadrant_chat_reads r ON r.user_id = ?
                WHERE m.channel = 'REFERENT_ENCADRANT' AND m.sender_user_id <> ?
                AND m.id > COALESCE(r.last_message_id, 0)
                """, Long.class, userId, userId);
    }

    public void markReferentEncadrantRead(Long userId, Long messageId) {
        jdbc.update("""
                INSERT INTO referent_encadrant_chat_reads (user_id, last_message_id) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE
                SET last_message_id = GREATEST(referent_encadrant_chat_reads.last_message_id, EXCLUDED.last_message_id)
                """, userId, messageId);
    }

    public void markCommunicationRead(Long userId, Long messageId) {
        jdbc.update("""
                INSERT INTO communication_chat_reads (user_id, last_message_id) VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE
                SET last_message_id = GREATEST(communication_chat_reads.last_message_id, EXCLUDED.last_message_id)
                """, userId, messageId);
    }

    public Map<Long, Long> unreadCounts(Long userId, List<Long> activityIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (activityIds.isEmpty()) return counts;
        String placeholders = String.join(",", java.util.Collections.nCopies(activityIds.size(), "?"));
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(userId);
        parameters.add(userId);
        parameters.addAll(activityIds);
        jdbc.query("""
                SELECT m.activite_id, COUNT(*) AS unread_count
                FROM activity_chat_messages m
                LEFT JOIN activity_chat_reads r ON r.activite_id = m.activite_id AND r.user_id = ?
                WHERE m.sender_user_id <> ? AND m.id > COALESCE(r.last_message_id, 0)
                AND m.activite_id IN (""" + placeholders + ") GROUP BY m.activite_id",
                rs -> { counts.put(rs.getLong("activite_id"), rs.getLong("unread_count")); },
                parameters.toArray());
        return counts;
    }

    public void markRead(Long userId, Long activityId, Long messageId) {
        // GREATEST prevents a delayed request or another tab from moving the cursor backwards.
        jdbc.update("""
                INSERT INTO activity_chat_reads (user_id, activite_id, last_message_id) VALUES (?, ?, ?)
                ON CONFLICT (user_id, activite_id) DO UPDATE
                SET last_message_id = GREATEST(activity_chat_reads.last_message_id, EXCLUDED.last_message_id)
                """, userId, activityId, messageId);
    }

    public Map<Long, Long> sectionUnreadCounts(Long userId, List<Long> chatIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (chatIds.isEmpty()) return counts;
        String placeholders = String.join(",", java.util.Collections.nCopies(chatIds.size(), "?"));
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(userId);
        parameters.add(userId);
        parameters.addAll(chatIds);
        jdbc.query("""
                SELECT m.section_chat_id, COUNT(*) AS unread_count
                FROM activity_chat_messages m
                LEFT JOIN section_chat_reads r ON r.section_chat_id = m.section_chat_id AND r.user_id = ?
                WHERE m.sender_user_id <> ? AND m.id > COALESCE(r.last_message_id, 0)
                AND m.section_chat_id IN (""" + placeholders + ") GROUP BY m.section_chat_id",
                rs -> { counts.put(rs.getLong("section_chat_id"), rs.getLong("unread_count")); },
                parameters.toArray());
        return counts;
    }

    public void markSectionRead(Long userId, Long sectionChatId, Long messageId) {
        jdbc.update("""
                INSERT INTO section_chat_reads (user_id, section_chat_id, last_message_id) VALUES (?, ?, ?)
                ON CONFLICT (user_id, section_chat_id) DO UPDATE
                SET last_message_id = GREATEST(section_chat_reads.last_message_id, EXCLUDED.last_message_id)
                """, userId, sectionChatId, messageId);
    }
}
