package com.gestion.adhesion.utils;

import java.util.List;

public class Constantes {

    public static final List<String> STATUTS_VALIDES = List.of("Validée", "Validée, en attente du certificat médical", "Licence T", "Retour Comité", "Licence générée", "Validée groupement sportif");
    public static final List<String> STATUTS_ENCOURS = List.of("Attente validation adhérent", "Attente validation secrétariat", "Attente licence en ligne");

    public static final List<String> STATUTS_ENATTENTE = List.of("Sur liste d'attente");

    public static final List<String> STATUTS_ANNULES = List.of("Annulée");

}
