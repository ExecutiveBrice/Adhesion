package com.wild.corp.adhesion.models.resources;

import lombok.Data;

import java.util.List;
@Data
public class Horaire {

    private  Boolean bold;
    private  Boolean checked;

    private Long id;
    private String role;
    private Boolean indent;
    private  String    nom;
    private  Integer    ordre;
    private  String    text;
}
