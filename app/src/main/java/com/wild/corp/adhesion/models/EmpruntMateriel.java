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

    private Long emprunteurId;

    @Column(length = 511)
    private String emprunteurNom;

    // Null pour un emprunt ; true/false pour un changement de statut HS.
    private Boolean horsService;

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
