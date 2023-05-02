package com.gestion.user.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "adhesions")
public class Adhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer tarif;

    private LocalDate saison;

    private String typeReglement;

    private boolean validPaiementSecretariat;

    private boolean validDocumentSecretariat;

    private String statutActuel;

    private String remarqueSecretariat;

    private boolean inscrit;

    private boolean flag;

    private LocalDate dateAjoutPanier;

    private LocalDate dateChangementStatut;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Accord> accords = new ArrayList<>();

    @ManyToOne
    @JsonIgnoreProperties({"adhesions"})
    private Activite activite;

    @ManyToOne
    @JsonIgnoreProperties({"adhesions","tribu" })
    @ToString.Exclude
    private Adherent adherent;

}
