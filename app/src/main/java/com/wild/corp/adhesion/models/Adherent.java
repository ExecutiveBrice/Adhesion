package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(of = {"id"})
@Entity
@NoArgsConstructor
@Table(name = "adherents",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class Adherent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prenom;

    private String nom;

    private String genre;

    private String telephone;

    private LocalDate naissance;

    private String lieuNaissance;

    private String adresse;

    private String codePostal;

    private String ville;

    private Boolean adresseRepresentant;

    private Boolean telephoneRepresentant;

    private Boolean emailRepresentant;

    private Boolean mineur;

    private Boolean completAdhesion;

    @OneToMany(mappedBy = "adherent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Accord> accords = new ArrayList<>();

    @ManyToOne
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "activitesNm1", "tribu",  "documents", "derniereModifs", "derniereVisites", "representant"})
    private Adherent representant;

    @JsonIgnore
    @OneToMany(mappedBy = "adherentModif", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> derniereModifs = new ArrayList<>();

    @OneToMany(mappedBy = "adherentVisite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> derniereVisites = new ArrayList<>();

    @ManyToOne
    @JsonIgnoreProperties({"adherents", "autorisations"})
    private Tribu tribu;

    @OneToMany(mappedBy = "adherent")
    @JsonIgnoreProperties({"paiements", "derniereModifs", "derniereVisites", "surClassement",  "adherent"})
    private Set<Adhesion> adhesions = new HashSet<>();

    @OneToMany(mappedBy = "adherent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"adherent", "tribu"})
    private List<ActiviteNm1> activitesNm1 = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    private Set<Activite> cours = new HashSet<>();

    @OneToOne
    @JsonIgnoreProperties({"adherent", "tokens"})
    private User user;

}
