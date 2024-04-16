package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(	name = "tribus",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "id"),
        })
public class Tribu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @OneToMany(mappedBy="tribu")
    @ToString.Exclude
    @JsonIgnoreProperties({"tribu", "cours", "derniereModifs", "derniereVisites"})
    private Set<Adherent> adherents = new HashSet<>();

    @OneToMany(mappedBy="tribu")
    @JsonIgnoreProperties({"tribu", "adherent"})
    private List<ActiviteNm1> autorisations;

    public Tribu(UUID uuid){
        this.uuid = uuid;
    }
}
