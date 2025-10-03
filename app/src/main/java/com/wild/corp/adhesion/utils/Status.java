package com.wild.corp.adhesion.utils;

public enum Status {

    VALIDEE("Validée"),
    ATTENTE_ADHERENT("Attente validation adhérent"),
    ATTENTE_SECRETARIAT("Attente validation secrétariat"),
    ATTENTE_CREATION_LICENCE("Attente création licence"),
    ATTENTE_LICENCE_EN_LIGNE("Licence FFBB à compléter"),
    LICENCE_GENEREE("Licence générée"),
    LICENCE_T("Licence T"),
    ATTENTE_CERFTIF("Validée, en attente du certificat médical"),
    VALIDEE_GROUPEMENT_SPORTIF("Validée groupement sportif"),
    LISTE_ATTENTE("Sur liste d'attente"),
    RETOUR_ALOD("Retour ALOD Basket"),
    RETOUR_COMITE("Retour Comité"),

    ANNULEE("Annulée");

    public final String label;

    private Status(String label) {
        this.label = label;
    }

}
