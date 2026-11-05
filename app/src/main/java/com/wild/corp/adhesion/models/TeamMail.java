package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "team_mails") @Getter @Setter
public class TeamMail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;
    @Column(nullable = false, unique = true, length = 64)
    private String cle;
    @Column(nullable = false)
    private int revision;
    @Column(nullable = false, length = 255)
    private String destinataire;
    @Column(nullable = false, length = 200)
    private String sujet;
    @Column(nullable = false, columnDefinition = "text")
    private String contenu;
    @Column(nullable = false, length = 20)
    private String etat = "PENDING";
    @Column(nullable = false)
    private boolean annulation;

    @Column(nullable = false, length = 20)
    private String type;

    @ElementCollection
    @CollectionTable(name = "team_mail_convocations", joinColumns = @JoinColumn(name = "mail_id"))
    @Column(name = "convocation_id", nullable = false)
    private java.util.Set<Long> convocations = new java.util.HashSet<>();
}
