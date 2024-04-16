package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "accords",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,   allowSetters = true)
public class Accord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nom;

    private String title;

    @Column(columnDefinition="TEXT")
    private String text;

    private String valide;

    private String refus;

    private Boolean refusable;

    private Boolean etat;

    private LocalDate datePassage;

    public Accord(String nom, String title,  String valide, String refus, Boolean refusable, String text) {
        this.title = title;
        this.nom = nom;
        this.text = text;
        this.valide = valide;
        this.refus = refus;
        this.refusable = refusable;
        this.etat = true;
        this.datePassage = null;
    }
}
