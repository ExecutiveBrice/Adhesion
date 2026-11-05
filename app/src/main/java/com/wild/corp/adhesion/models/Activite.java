package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne
    @JoinColumn(name = "section_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Section section;

    private String nom;

    private String lien;

    private Integer tarif;

    private Integer nbPlaces;

    private Integer ageMin;

    private Integer ageMax;

    private String genre;

    @Column(name = "salle")
    @JsonIgnore
    private String salleTexte;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Salle salle;

    private Boolean reinscription;

    private Boolean globaleSpecifique;

    private boolean priseEnCharge;

    private boolean autorisationParentale;

    private boolean vieClub;

    private boolean charteAmicale;


    private boolean certificatMedical;

    private boolean complete;

    @Column(name = "majoration")
    private boolean majoration;

    private Integer dureeVieCertif;

    @Transient
    private Long nbAdhesionsEnCours;

    @Transient
    private Long nbAdhesionsCompletes;

    @Transient
    private Long montantCollecte;

    @Transient
    private Long nbAdhesionsAttente;

    @Transient
    private Long nbEvenementsRealises;

    @Transient
    private Long nbEvenementsTotal;

    public record EquipeResume(Long id, String nom, long membres) { }

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private List<EquipeResume> equipes = new ArrayList<>();

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private List<com.wild.corp.adhesion.models.resources.SeanceCalendrierResponse> evenementsSemaineProchaine = new ArrayList<>();

    @OneToMany(mappedBy="activite")
    @JsonIgnore
    private Set<Adhesion> adhesions = new HashSet<>();

    @OneToMany(mappedBy = "surClassement")
    @JsonIgnore
    private Set<Adhesion> sousClassement = new HashSet<>();

    @ManyToMany(mappedBy="cours", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Set<Adherent> profs = new HashSet<>();

    @ManyToMany(mappedBy = "activitesReferent")
    @JsonIgnoreProperties({"cours", "activitesReferent", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Set<Adherent> referents = new HashSet<>();

    @OneToMany(mappedBy = "activite", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Seance> seances = new ArrayList<>();

    public String getNomSalle() {
        return salle != null ? salle.getNom() : salleTexte;
    }

    /** Compatibility names derived from the section for older exports. */
    @JsonIgnore
    public String getGroupe() {
        return section != null ? section.getType() : null;
    }

    @JsonIgnore
    public String getGroupeFiltre() {
        return section != null ? section.getNom() : null;
    }

    /** Compatibility label for previous-year exports; dates now belong to events. */
    public String getHoraire() { return ""; }

}
