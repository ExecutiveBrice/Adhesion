package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = {"id"})
@Entity
@NoArgsConstructor
@Table(name = "seance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties({"adhesions", "profs"})
    private Activite activite;

    /**
     * Salle effectivement prévue pour cette séance. Elle est initialisée avec
     * la salle de l'activité lors de la planification, puis peut évoluer sans
     * modifier les autres séances.
     */
    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Salle salle;

    private ESeance etatSeance;

    private String causeAnnulation;

    private LocalDateTime debut;

    private LocalDateTime fin;

    private String commentaire;

    /** Short label inherited from the weekly schedule for calendar display. */
    @Column(length = 100)
    private String descriptif;

    @OneToMany(mappedBy = "seance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Presence> presences = new ArrayList<>();

}
