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

    public Map<Long, Long> unreadCounts(Long userId, List<Long> chatIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (chatIds.isEmpty()) return counts;
        String placeholders = String.join(",", java.util.Collections.nCopies(chatIds.size(), "?"));
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(userId);
        parameters.add(userId);
        parameters.addAll(chatIds);
        jdbc.query("""
                SELECT m.chat_id, COUNT(*) AS unread_count
                FROM chat_messages m
                LEFT JOIN chat_reads r ON r.chat_id = m.chat_id AND r.user_id = ?
                WHERE m.sender_user_id <> ? AND m.id > COALESCE(r.last_message_id, 0)
                AND m.chat_id IN (""" + placeholders + ") GROUP BY m.chat_id",
                rs -> { counts.put(rs.getLong("chat_id"), rs.getLong("unread_count")); },
                parameters.toArray());
        return counts;
    }

    public void markRead(Long userId, Long chatId, Long messageId) {
        jdbc.update("""
                INSERT INTO chat_reads (user_id, chat_id, last_message_id) VALUES (?, ?, ?)
                ON CONFLICT (user_id, chat_id) DO UPDATE
                SET last_message_id = GREATEST(chat_reads.last_message_id, EXCLUDED.last_message_id)
                """, userId, chatId, messageId);
    }
}
