package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/** A recurring weekly slot for an activity. */
@Getter
@Setter
@Entity
@Table(name = "planifications_hebdomadaires")
public class PlanificationHebdomadaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activite_id", nullable = false)
    @JsonIgnore
    private Activite activite;

    private DayOfWeek jour;

    private LocalTime horaireDebut;

    private Long duree;

    @Column(length = 100)
    private String descriptif;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Salle salle;

    /** Teachers assigned specifically to this weekly session category. */
    @ManyToMany
    @JoinTable(name = "planification_professeurs",
            joinColumns = @JoinColumn(name = "planification_id"),
            inverseJoinColumns = @JoinColumn(name = "adherent_id"))
    @JsonIgnoreProperties({"cours", "activitesReferent", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Set<Adherent> profs = new HashSet<>();

    /** Coordinators assigned specifically to this weekly session category. */
    @ManyToMany
    @JoinTable(name = "planification_referents",
            joinColumns = @JoinColumn(name = "planification_id"),
            inverseJoinColumns = @JoinColumn(name = "adherent_id"))
    @JsonIgnoreProperties({"cours", "activitesReferent", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Set<Adherent> referents = new HashSet<>();
}
