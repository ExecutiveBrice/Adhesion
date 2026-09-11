package com.wild.corp.adhesion.models.resources;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
public class AdherentExport {

    private Long id;

    private String prenom;
    private String nom;
    private String genre;
    private LocalDate naissance;
    private String lieuNaissance;

    private String adresse;
    private String cp;
    private String ville;

    private String email;
    private String telephone;
    private String majorite;

    private String activite1;
    private String statutAdhesion1;
    private String activite2;
    private String statutAdhesion2;
    private String activite3;
    private String statutAdhesion3;
    private String activite4;
    private String statutAdhesion4;
    private String activite5;
    private String statutAdhesion5;
    private String activitesNm1;

}
