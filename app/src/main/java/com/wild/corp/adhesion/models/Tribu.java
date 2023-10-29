package com.wild.corp.adhesion.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
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
    @JsonIgnoreProperties({"tribu","user", "cours"})
    private Set<Adherent> adherents = new HashSet<>();

    public Tribu(UUID uuid){
        this.uuid = uuid;
    }
}
