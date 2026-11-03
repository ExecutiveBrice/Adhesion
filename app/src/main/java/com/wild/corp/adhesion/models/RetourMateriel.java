package com.wild.corp.adhesion.models;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class RetourMateriel {
    @Column(nullable = false) private int quantite;
    @Column(nullable = false) private OffsetDateTime dateRetour;
    private Long rapporteParId;
    @Column(length = 511) private String rapporteParNom;
}
