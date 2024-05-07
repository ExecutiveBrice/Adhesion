package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(of = {"id"})
@Entity
@Data
@Table(	name = "activitesNm1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActiviteNm1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String groupe;
    private String groupeFiltre;
    private String nom;
    private Integer tarif;
    private String horaire;
    private String salle;

    @ManyToOne
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "activitesNm1", "user", "tribu", "derniereModifs", "derniereVisites"})
    private Adherent adherent;

    @ManyToOne
    private Tribu tribu;
}
