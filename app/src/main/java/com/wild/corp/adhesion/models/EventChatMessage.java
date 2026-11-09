package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "event_chat_messages")
@Getter @Setter @NoArgsConstructor
public class EventChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;
    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;
    @Column(name = "sender_name", nullable = false, length = 255)
    private String senderName;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
