package com.wild.corp.adhesion.models.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wild.corp.adhesion.models.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
public class AdherentFlat {

    private Long id;

    private String prenom;

    private String nom;
    private String nomPrenom;


    private String email;

    private String telephone;

    private LocalDate naissance;

    private String lieuNaissance;

    private String adresse;

    private String adhesions;


    private UUID tribuId;



    private List<Accord> accords = new ArrayList<>();


}
