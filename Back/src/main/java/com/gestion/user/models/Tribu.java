package com.gestion.user.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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

    @OneToMany(mappedBy="tribu")
    @ToString.Exclude
    @JsonIgnoreProperties({"tribu","user", "cours"})
    private Set<Adherent> adherents = new HashSet<>();

}
