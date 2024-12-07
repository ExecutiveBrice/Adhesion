package com.wild.corp.adhesion.models;

import com.wild.corp.adhesion.models.resources.AdherentLite;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class AdhesionLite {

    private Long id;

    private Integer tarif;

    private Integer position;

    private Boolean dejaLicencie;

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

    private List<Paiement> paiements = new ArrayList<>();

    private List<Accord> accords = new ArrayList<>();

    private ActiviteLite activite;

    private AdherentLite adherent;

    private List<Notification> derniereModifs = new ArrayList<>();

    private List<Notification> derniereVisites = new ArrayList<>();
}
