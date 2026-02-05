package com.wild.corp.adhesion.models.resources;


import lombok.Data;

import java.util.List;

@Data
public class Groupe {


   private Boolean bold;
    private  Boolean checked;
    private   List<Horaire> horaires;

    private   Long id;
    private   Boolean indent;
    private   String    nom;
    private  Integer    ordre;
    private  String    text;
}
