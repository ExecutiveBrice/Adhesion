package com.gestion.adhesion.models;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class AdherentLite {

    private Long id;

    private String prenom;

    private String nom;

    private String prenomNom;

    private String email;

    private String adresse;

    private String telephone;

    private String nomLegal;

    private String prenomLegal;

    private String telLegal;

    private boolean mineur = false;

    private String lieuNaissance;

    private LocalDate naissance;

    private Long tribuId;

    private Integer tribuSize;

    private boolean referent = false;

    private List<Accord> accords = new ArrayList<>();

    private Set<AdhesionLite> adhesions = new HashSet<>();

}
