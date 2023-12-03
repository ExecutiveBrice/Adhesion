package com.wild.corp.adhesion.models;

import lombok.*;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
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

    private boolean referent = false;

    private String adresse;

    private boolean adresseReferent = true;

    private boolean telephoneReferent = true;

    private boolean emailReferent = true;

    private boolean mineur = false;

    private String nomLegal;

    private String prenomLegal;

    private boolean legalReferent = true;

    private boolean completReferent = false;

    private boolean completAdhesion = false;


    private String nomPrenom;

    private String emailLegal;
    private String adresseLegal;
    private String telLegal;

    private String statut;

    private String commentaire;

    private boolean flag;
    private boolean paiement;

    private UUID tribuId;

    private Integer tribuSize;

    private boolean documentPresent = false;
    private String lien;


    private List<Accord> accords = new ArrayList<>();

    private String activites;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();

}
