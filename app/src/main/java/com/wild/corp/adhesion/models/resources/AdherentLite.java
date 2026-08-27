package com.wild.corp.adhesion.models.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wild.corp.adhesion.models.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class AdherentLite {

    /**
     * Créateur explicite pour Jackson. Ne pas dépendre de la génération Lombok
     * ici : selon le compilateur utilisé pour lancer l'application, Jackson ne
     * voyait que le constructeur toutes propriétés.
     */
    @JsonCreator
    public AdherentLite() {
    }

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

    // Les adhésions sont renvoyées à l'interface, mais ne sont jamais mises à
    // jour par cet endpoint. Les ignorer à l'entrée évite de désérialiser la
    // structure complète d'une activité (notamment sa salle, qui est un objet).
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<AdhesionLite> adhesions;

    private List<Accord> accords = new ArrayList<>();

    @JsonIgnoreProperties({"adhesions", "accords"})
    private AdherentLite representant;

    @JsonIgnoreProperties({"adherent", "tribu"})
    private List<ActiviteNm1> activitesNm1;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();

    // Le client transmet parfois l'objet User complet, qui contient en retour
    // l'adhérent. Cette référence circulaire n'est pas utile à la mise à jour :
    // seul le nom d'utilisateur est lu par le service.
    @JsonIgnoreProperties({"adherent"})
    private UserLite user;
}
