package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @ManyToOne
    @JoinColumn(name = "seance_id")
    @JsonIgnore
    @JsonIgnoreProperties({"activite", "presences"})
    private Seance seance;

    @ManyToOne
    @JsonIgnoreProperties({"activite", "presences"})
    private Adhesion adhesion;

    private Boolean presence;

    private Boolean presencePrevue;

    private LocalDate dateModification;

    public Presence() {
        this.presence = null;
        this.presencePrevue = null;
        this.dateModification = LocalDate.now();
    }

    @JsonProperty("seanceId")
    public Long getSeanceId() {
        return seance == null ? null : seance.getId();
    }
}
