package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wild.corp.adhesion.utils.Status;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = {"id"})
@Entity
@NoArgsConstructor
@Table(name = "adhesions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class Adhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer tarif;

    private Integer position;

    private Boolean dejaLicencie;

    private Boolean validPaiementSecretariat;

    private Boolean validDocumentSecretariat;

    private String statutActuel;

    private String remarqueSecretariat;

    private Boolean inscrit;

    private Boolean flag;

    private Boolean majoration;

    private Boolean rappel;

    private LocalDate dateAjoutPanier;

    private LocalDate dateChangementStatut;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Paiement> paiements = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "adhesionModif", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> derniereModifs = new ArrayList<>();

    @OneToMany(mappedBy = "adhesionVisite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> derniereVisites = new ArrayList<>();

    @OneToMany(mappedBy = "adhesion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Accord> accords = new ArrayList<>();

    @ManyToOne
    @JsonIgnoreProperties({"adhesions", "profs"})
    private Activite activite;

    @ManyToOne
    @JsonIgnoreProperties({"adhesions", "profs"})
    private Activite surClassement;

    @ManyToOne
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "activitesNm1"})
    @ToString.Exclude
    private Adherent adherent;

    private static final List<String> list_valid = List.of(Status.VALIDEE.label, Status.ATTENTE_CERFTIF.label, Status.LICENCE_T.label, Status.LICENCE_GENEREE.label, Status.VALIDEE_GROUPEMENT_SPORTIF.label);
    private static final List<String> list_encours = List.of(Status.ATTENTE_ADHERENT.label, Status.ATTENTE_SECRETARIAT.label, Status.ATTENTE_LICENCE_EN_LIGNE.label);
    private static final List<String> list_attente = List.of(Status.LISTE_ATTENTE.label);

    public boolean isValide(){
        return list_valid.contains(this.statutActuel);
    }
    public boolean isEnAttente(){
        return list_attente.contains(this.statutActuel);
    }
    public boolean isEnCours(){
        return list_encours.contains(this.statutActuel);
    }
}
