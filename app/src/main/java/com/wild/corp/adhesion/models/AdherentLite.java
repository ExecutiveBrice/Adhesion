package com.wild.corp.adhesion.models;

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

    private List<String> documents;

    private Boolean mineur = false;

    private Boolean completAdhesion = false;

    private AdherentLite representant;

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

    private Set<AdhesionLite> adhesions;

    private List<Accord> accords = new ArrayList<>();

    private String activites;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();

    private UserLite user;
}
