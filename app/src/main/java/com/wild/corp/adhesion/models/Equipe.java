package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

@Entity
@Table(name = "equipes", uniqueConstraints = @UniqueConstraint(columnNames = {"activite_id", "nom"}))
@Getter @Setter
public class Equipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_id", nullable = false)
    private Activite activite;
    @Column(nullable = false, length = 100)
    private String nom;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean archivee;
    @ManyToMany
    @JoinTable(name = "equipe_encadrants", joinColumns = @JoinColumn(name = "equipe_id"), inverseJoinColumns = @JoinColumn(name = "adherent_id"))
    private Set<Adherent> encadrants = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "equipe_referents", joinColumns = @JoinColumn(name = "equipe_id"), inverseJoinColumns = @JoinColumn(name = "adherent_id"))
    private Set<Adherent> referents = new HashSet<>();
    @OneToMany(mappedBy = "equipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AffectationEquipe> affectations = new ArrayList<>();
}
