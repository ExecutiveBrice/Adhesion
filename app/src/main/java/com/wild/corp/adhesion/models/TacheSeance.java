package com.wild.corp.adhesion.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Entity
@Table(name = "taches_seance")
public class TacheSeance {
    public static final String ICONE_PAR_DEFAUT = "clipboard-check";
    public static final String COULEUR_PAR_DEFAUT = "#176B4A";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nom;

    @Column(nullable = false, length = 50)
    private String icone = ICONE_PAR_DEFAUT;

    @Column(nullable = false, length = 7)
    private String couleur = COULEUR_PAR_DEFAUT;
}
