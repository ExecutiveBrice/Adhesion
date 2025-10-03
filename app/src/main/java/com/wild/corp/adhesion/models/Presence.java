package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = {"id"})
@Entity@Table(name = "presence",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,  allowSetters = true)
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long seanceId;

    @ManyToOne
    @JsonIgnoreProperties({"activite", "presences"})
    private Adhesion adhesion;

    private Boolean presence;

    private LocalDate dateModification;

    public Presence() {
        this.presence = false;
        this.dateModification = LocalDate.now();
    }
}
