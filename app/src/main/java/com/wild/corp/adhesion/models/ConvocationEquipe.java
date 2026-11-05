package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
@Table(name = "convocations_equipe", uniqueConstraints = @UniqueConstraint(columnNames = {"seance_id", "adherent_id"}))
public class ConvocationEquipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;
    @ManyToOne(optional = false) @JoinColumn(name = "adherent_id", nullable = false)
    private Adherent adherent;
    @ManyToOne(optional = false) @JoinColumn(name = "adhesion_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Adhesion adhesion;
    @Column(nullable = false)
    private boolean selectionnee;
    private LocalDateTime publication;
    private Boolean reponse;
    private LocalDateTime dateReponse;
    private Long reponduPar;

    @ElementCollection
    @CollectionTable(name = "convocation_reponses", joinColumns = @JoinColumn(name = "convocation_id"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private java.util.List<ReponseConvocation> historique = new java.util.ArrayList<>();
}
