package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_id")
    private Activite activite;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatTarget cible = ChatTarget.SECTION;

    @Column(nullable = false, length = 100)
    private String nom;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ChatPermission> permissions = new ArrayList<>();

    public String targetLabel() {
        return switch (cible) {
            case SECTION -> "Section · " + section.getNom();
            case ACTIVITE -> "Activité · " + activite.getNom();
            case ASSOCIATION -> "Global";
        };
    }
}
