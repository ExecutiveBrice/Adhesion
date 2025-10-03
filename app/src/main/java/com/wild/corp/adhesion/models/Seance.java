package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    private ESeance etatSeance;

    private String causeAnnulation;

    private LocalDate dateSeance;

    private String commentaire;

    @OneToMany(mappedBy = "seanceId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Presence> presences = new ArrayList<>();

}
