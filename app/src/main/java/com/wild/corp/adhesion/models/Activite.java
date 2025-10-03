package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
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

    private Integer ageMin;

    private Integer ageMax;

    private String genre;

    private String horaire;

    private String salle;

    private Boolean reinscription;

    private boolean priseEnCharge;

    private boolean autorisationParentale;

    private boolean vieClub;

    private boolean charteAmicale;


    private DayOfWeek jour;


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

    @OneToMany(mappedBy="activite")
    @JsonIgnore
    private Set<Adhesion> adhesions = new HashSet<>();

    @OneToMany(mappedBy = "surClassement")
    @JsonIgnore
    private Set<Adhesion> sousClassement = new HashSet<>();

    @ManyToMany(mappedBy="cours", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Set<Adherent> profs = new HashSet<>();

    @OneToMany(mappedBy = "activite", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Seance> seances = new ArrayList<>();

}
