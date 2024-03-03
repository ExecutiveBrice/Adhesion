package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private LocalDate saison;

    private LocalDate dateReglement;

    private String typeReglement;

    private Boolean validPaiementSecretariat;

    private Boolean validDocumentSecretariat;

    private String statutActuel;

    private String remarqueSecretariat;

    private Boolean inscrit;

    private Boolean flag;

    private Boolean rappel;

    private LocalDate dateAjoutPanier;

    private LocalDate dateChangementStatut;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Paiement> paiements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<Notification> derniereModifs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<Notification> derniereVisites = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<Accord> accords = new ArrayList<>();

    @ManyToOne
    @JsonIgnoreProperties({"adhesions", "profs"})
    private Activite activite;

    @ManyToOne
    @JsonIgnoreProperties({"cours", "accords", "adhesions", "user", "tribu", "derniereModifs", "derniereVisites"})
    @ToString.Exclude
    private Adherent adherent;

    private static final List<String> list_valid = List.of("Validée", "Validée, en attente du certificat médical");
    private static final List<String> list_G_encours = List.of("Attente validation adhérent", "Attente validation secrétariat");

    private static final List<String> list_B_valid = List.of("Validée", "Validée, en attente du certificat médical", "Licence T", "Retour Comité", "Licence générée", "Validée groupement sportif");
    private static final List<String> list_B_encours = List.of("Attente validation adhérent", "Attente validation secrétariat", "Attente licence en ligne");

    private static final List<String> list_attente = List.of("Sur liste d'attente");

    public boolean isValide(){
        return list_valid.contains(getStatutActuel());
    }
    public boolean isEnAttente(){
        return list_attente.contains(getStatutActuel());
    }
    public boolean isEnCours(){
        return list_G_encours.contains(getStatutActuel());
    }
}
