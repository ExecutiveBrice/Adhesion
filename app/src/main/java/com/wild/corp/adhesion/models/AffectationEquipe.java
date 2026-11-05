package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "affectations_equipe") @Getter @Setter
public class AffectationEquipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;
    @ManyToOne(optional = false) @JoinColumn(name = "adhesion_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Adhesion adhesion;
    @Column(nullable = false)
    private LocalDate debut;
    private LocalDate fin;
}
