package com.gestion.user.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static javax.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "activites")
public class Activite {

    @Id
    @SequenceGenerator(
            name = "activite_sequence",
            sequenceName = "activite_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "activite_sequence"
    )
    private Long id;

    private String groupe;

    private String nom;
    private String lien;
    private Integer tarif;

    private Integer nbPlaces;

    private String horaire;

    private String salle;

    private Boolean pourEnfant;

    private boolean priseEnCharge;

    private boolean autorisationParentale;

    private boolean vieClub;

    private boolean certificatMedical;

    @Transient
    private int nbAdherentTotal;

    @OneToMany(mappedBy="activite", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<Adhesion> adhesions = new HashSet<>();

    @ManyToMany
    private Set<Adherent> profs = new HashSet<>();
}
