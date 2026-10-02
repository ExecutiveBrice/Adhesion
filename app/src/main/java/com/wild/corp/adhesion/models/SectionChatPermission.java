package com.wild.corp.adhesion.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "section_chat_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"section_chat_id", "role"}))
public class SectionChatPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_chat_id", nullable = false)
    private SectionChat chat;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false)
    private boolean ecriture;
}
