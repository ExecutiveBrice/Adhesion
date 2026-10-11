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
@Table(name = "materiel", indexes = @Index(name = "idx_materiel_zonage", columnList = "zonage_id"))
public class Materiel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zonage_id", nullable = false)
    private Zonage zonage;

    // Identité figée à l'emprunt : elle reste lisible si le compte est ensuite supprimé.
    private Long emprunteurId;

    @Column(length = 511)
    private String emprunteurNom;

    private OffsetDateTime dateEmprunt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean horsService;
}
