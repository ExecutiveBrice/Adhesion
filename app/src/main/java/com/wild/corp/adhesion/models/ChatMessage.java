package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "sender_name", nullable = false, length = 255)
    private String senderName;

    /** Role shown beside the author's name at the time the message was sent. */
    @Column(name = "sender_role", length = 20)
    private String senderRole;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
