package com.gestion.user.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "adherents",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
public class Adherent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prenom;

    private String nom;

    private String genre;

    private String email;

    private String telephone;

    private LocalDate naissance;

    private String lieuNaissance;

    private boolean referent = false;

    private String adresse;

    private boolean adresseReferent = true;

    private boolean telephoneReferent = true;

    private boolean emailReferent = true;

    private boolean mineur = false;

    private String nomLegal;

    private String prenomLegal;

    private boolean legalReferent = true;

    private boolean completReferent = false;

    private boolean completAdhesion = false;

    @Transient
    private Long tribuId;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Accord> accords = new ArrayList<>();

    @ManyToOne
    private Tribu tribu;

    @OneToMany(mappedBy="adherent", cascade = CascadeType.ALL)
    private Set<Adhesion> adhesions = new HashSet<>();

    @ManyToMany
    private Set<Activite> cours = new HashSet<>();

    @OneToOne
    @JsonIgnore
    private User user;
}
