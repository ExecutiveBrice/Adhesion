package com.wild.corp.adhesion.models;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class ActiviteLite {

    private Long id;
    private String nom;
    private String lien;
    private String horaire;
    private String salle;
    private String groupe;
    private Set<AdherentLite> adherents = new HashSet<>();

}
