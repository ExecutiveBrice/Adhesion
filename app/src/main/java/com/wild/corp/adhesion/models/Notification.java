package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "Notification",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
@JsonIgnoreProperties(ignoreUnknown = true,   allowSetters = true)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    @ManyToOne
    @JsonIgnoreProperties({"adherent","tokens","notifs" })
    private User user;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Adherent adherentModif;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Adherent adherentVisite;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Adhesion adhesionModif;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Adhesion adhesionVisite;
}
