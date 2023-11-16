package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;


import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "activites")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupe;

    private String groupeFiltre;

    private String nom;
    private String lien;
    private Integer tarif;

    private Integer nbPlaces;

    private String horaire;

    private String salle;

    private Boolean pourEnfant;

    private boolean priseEnCharge;

    private boolean autorisationParentale;

    private boolean vieClub;

    private boolean certificatMedical;
    private boolean complete;
    private Integer dureeVieCertif;

    @Transient
    private Long nbAdhesionsEnCours;

    @Transient
    private Long nbAdhesionsCompletes;

    @Transient
    private Long montantCollecte;

    @Transient
    private Long nbAdhesionsAttente;

    @OneToMany(mappedBy="activite", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<Adhesion> adhesions = new HashSet<>();

    @ManyToMany(mappedBy="cours", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "user", "tribu"})
    private Set<Adherent> profs = new HashSet<>();
}
