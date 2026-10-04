package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


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


    private DayOfWeek jour;

    private LocalTime horaireDebut;

    private Long duree;

    /**
     * Weekly schedules. The legacy day/time/duration fields above are kept so
     * existing exports and integrations remain compatible.
     */
    @OneToMany(mappedBy = "activite", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("jour ASC, horaireDebut ASC")
    private List<PlanificationHebdomadaire> planificationsHebdomadaires = new ArrayList<>();

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
    private Long nbSeancesRealisees;

    @Transient
    private Long nbSeancesTotal;

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

    /**
     * Compatibility label derived from the weekly schedules. It is no longer
     * persisted as a free-text activity field.
     */
    public String getHoraire() {
        if (planificationsHebdomadaires == null) {
            return "";
        }
        return planificationsHebdomadaires.stream()
                .filter(planification -> planification.getJour() != null || planification.getHoraireDebut() != null
                        || (planification.getDescriptif() != null && !planification.getDescriptif().isBlank()))
                .map(planification -> {
                    String jour = planification.getJour() == null ? "" : planification.getJour()
                            .getDisplayName(TextStyle.FULL, Locale.FRENCH);
                    String heure = planification.getHoraireDebut() == null ? "" : planification.getHoraireDebut()
                            .format(DateTimeFormatter.ofPattern("HH:mm"));
                    String descriptif = planification.getDescriptif() == null ? "" : planification.getDescriptif().trim();
                    return List.of(jour, heure, descriptif).stream()
                            .filter(element -> !element.isBlank())
                            .collect(Collectors.joining(" · "));
                })
                .collect(Collectors.joining(" / "));
    }

    @JsonIgnore
    public String getGroupe() {
        return section != null ? section.getType() : null;
    }

    @JsonIgnore
    public String getGroupeFiltre() {
        return section != null ? section.getNom() : null;
    }

}
