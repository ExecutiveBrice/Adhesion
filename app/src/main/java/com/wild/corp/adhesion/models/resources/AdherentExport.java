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
    private String majorite;

    private String activite1;
    private String activite2;
    private String activite3;
    private String activite4;
    private String activite5;

}
