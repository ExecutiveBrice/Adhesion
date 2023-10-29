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

    private String nomPrenom;

    private String email;

    private String adresse;

    private String telephone;

    private String nomLegal;

    private String prenomLegal;

    private String telLegal;

    private String statut;

    private String commentaire;

    private boolean flag;
    private boolean paiement;
    private boolean mineur = false;

    private String lieuNaissance;

    private LocalDate naissance;

    private UUID tribuId;

    private Integer tribuSize;

    private boolean referent = false;

    private List<Accord> accords = new ArrayList<>();

    private String activites;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();

}
