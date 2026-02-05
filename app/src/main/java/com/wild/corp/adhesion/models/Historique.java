package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "historique",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,   allowSetters = true)
public class Historique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String listing;
    @NotBlank
    private String subject;

    private Integer echecs;

    private Integer nombre;

    private LocalDateTime dateEnvoi;

    public Historique(String listing, String subject, Integer echecs, Integer nombre, LocalDateTime dateEnvoi) {
        this.listing = listing;
        this.subject = subject;
        this.echecs = echecs;
        this.nombre = nombre;
        this.dateEnvoi = dateEnvoi;
    }
}
