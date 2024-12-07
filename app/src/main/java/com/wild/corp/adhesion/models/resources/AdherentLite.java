package com.wild.corp.adhesion.models.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wild.corp.adhesion.models.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class AdherentLite {

    private Long id;

    private String prenom;

    private String nom;

    private String genre;

    private String email;

    private String telephone;

    private LocalDate naissance;

    private String lieuNaissance;

    private String adresse;

    private String codePostal;

    private String ville;

    private List<String> documents;

    private Boolean mineur = false;

    private Boolean completAdhesion = false;

    private Boolean adresseRepresentant;

    private Boolean telephoneRepresentant;

    private Boolean emailRepresentant;

    private String nomPrenom;

    private String statut;

    private String commentaire;

    private Boolean flag;

    private Boolean paiement;

    private UUID tribuId;

    private Integer tribuSize;

    private String lien;

    private String activites;

    private Set<AdhesionLite> adhesions;

    private List<Accord> accords = new ArrayList<>();

    @JsonIgnoreProperties({"adhesions", "accords"})
    private AdherentLite representant;

    @JsonIgnoreProperties({"adherent", "tribu"})
    private List<ActiviteNm1> activitesNm1;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();

    private UserLite user;
}
