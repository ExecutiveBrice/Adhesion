package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "emprunt_materiel", indexes = @Index(name = "idx_emprunt_materiel", columnList = "materiel_id,date_emprunt"))
public class EmpruntMateriel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 1")
    private int quantite = 1;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int quantiteRetournee = 0;

    @ElementCollection
    @CollectionTable(name = "retour_materiel", joinColumns = @JoinColumn(name = "emprunt_id"),
            indexes = @Index(name = "idx_retour_materiel_emprunt", columnList = "emprunt_id"))
    @OrderBy("dateRetour ASC")
    @Builder.Default
    private java.util.List<RetourMateriel> retours = new java.util.ArrayList<>();

    private Long emprunteurId;

    @Column(length = 511)
    private String emprunteurNom;

    // Null pour un emprunt ; true/false pour un changement de statut HS.
    private Boolean horsService;

    @Column(length = 2000)
    private String commentaire;

    @Column(nullable = false)
    private OffsetDateTime dateEmprunt;

    private OffsetDateTime dateRetour;
    private Long enregistreParId;
    @Column(length = 511)
    private String enregistreParNom;
    private Long rapporteParId;
    @Column(length = 511)
    private String rapporteParNom;
}
