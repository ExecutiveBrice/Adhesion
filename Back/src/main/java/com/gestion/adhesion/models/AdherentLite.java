package com.gestion.adhesion.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    private String email;

    private String telephone;

    private String nomLegal;

    private String prenomLegal;

    private String telLegal;

    private List<Accord> accords = new ArrayList<>();

}
