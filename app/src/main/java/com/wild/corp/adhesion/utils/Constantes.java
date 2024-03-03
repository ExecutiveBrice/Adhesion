package com.wild.corp.adhesion.utils;

import java.util.Map;

import static java.util.Map.entry;

public class Constantes {


    public static final String ACCORD_REGLEMENT_INTERIEUR = "Reglement Interieur";
    public static final String ACCORD_ATTESTATION_SANTE = "Attestation Sante";
    public static final String ACCORD_VIE_CLUB = "Vie du Club";
    public static final String ACCORD_AUTORISATION_PARENTALE = "Autorisation Parentale";
    public static final String ACCORD_PRISE_EN_CHARGE = "Prise en Charge";
    public static final Map<String, String> TEXTS_ACCORD = Map.ofEntries(
            entry(ACCORD_REGLEMENT_INTERIEUR, " Vous vous inscrivez à une activité au sein de l’ALOD (Amicale Laïque de L’Ouche Dinier). <br />" +
                    "<b>Vous pouvez consulter les statuts et toutes les informations concernant cette association sur" +
                    "  le site : <a href=\"https://www.alod.fr\" target=\"_blank\">www.alod.fr</a></b><br /> <br />" +
                    "Inscriptions et cours <br />" +
                    "Les inscriptions aux activités sont valables pour la période de septembre à juin. <br />" +
                    "1 cours d’essai est possible avant l’inscription selon les modalités de l’activité si le cours" +
                    "n’est pas complet. <br />" +
                    "Toute inscription est définitive<br />" +
                    "Vous pouvez participer à l’activité quand votre dossier d’inscription est complet.<br />" +
                    "Aucun remboursement ne pourra être demandé<br /><br />" +
                    "Seule la présentation au cours du premier trimestre d’un certificat médical de contre-" +
                    "indication précisant l’activité pratiquée, avec restitution de la carte de la FAL, pourra faire" +
                    "l’objet d’un remboursement partiel déduction faite des frais engagés (carte FAL, frais" +
                    "administratifs ALOD, cotisation 1er trimestre).<br />" +
                    "Les cas de force majeure justifiés (mutation professionnelle, déménagement,...) survenus au" +
                    "cours du 1er trimestre feront l’objet d’un remboursement partiel.<br />" +
                    "L’ALOD propose 25 à 30 cours par an en fonction du calendrier" +
                    "(jours fériés), des disponibilités des animateurs, etc. Un cours annulé sera reporté ou non" +
                    "selon les possibilités (disponibilités de l’animateur, de salles,...). En aucun cas, un" +
                    "remboursement ne pourra être demandé pour l’annulation de cours dans la limite des 25" +
                    "cours/an, ni en cas de circonstance(s) exceptionnelle(s).<br /><br />" +
                    "Fonctionnement des sections<br />" +
                    "Chaque section est représentée par un ou des référents responsables agréé(s) par le Conseil" +
                    "d’Administration. Ils mettent en application auprès des animateurs les décisions prises en" +
                    "Assemblée Générale et par le Conseil d’Administration.<br />" +
                    "En cas de litige dans une section, le Conseil d’Administration convoquera les référents et les" +
                    "animateurs et prendra les décisions adéquates.<br />" +
                    "A l’Assemblée Générale, l’animateur ou un responsable de la section peut présenter un" +
                    "compte rendu de la saison écoulée.<br /><br />" +
                    "Fonctionnement de l’Amicale<br />" +
                    "L’Amicale laïque de l’Ouche Dinier est administrée par un Conseil d’Administration.<br />" +
                    "Aucun membre du Conseil d’Administration ne peut se servir de ce titre s’il n’est délégué" +
                    "officiellement à cet effet. L’administrateur assiste régulièrement aux réunions du Conseil" +
                    "d’Administration. Des absences répétées sans excuse pourront entraîner l’exclusion après" +
                    "entretien entre l’intéressé et le Conseil d’Administration. Les représentants de l’Amicale dans" +
                    "différentes instances doivent être agréés par le Conseil d’Administration.<br />" +
                    "Une section ne peut s’ouvrir que sur décision du Conseil d’Administration.<br />"),
            entry(ACCORD_ATTESTATION_SANTE, "Le certificat médical n'est obligatoire<br />" +
                    "que dans le cas d'une réponse positive au questionnaire de santé<br />" +
                    "<a href=\"https://www.alod.fr/wp-content/uploads/2022/04/INSCRIPTION-Questionnaire_de_sante.pdf\" target=\"_blank\">Questionnaire de santé</a><br>" +
                    "Si vous avez répondu Non à toutes les questions du questionnaire,<br>" +
                    "vous pouvez valider cet accord d'attestation de votre bonne santé.<br>" +
                    "Sinon refusez le pour signifier au secrétariat que vous devrez fournir un certificat médical<br>"),
            entry(ACCORD_VIE_CLUB, "J'accepte également de participer,<br />" +
                    "dans la limite de mes possibilités et de manière collective,<br />" +
                    "aux déplacements de l'équipe,<br />" +
                    "aux responsabilités d'arbitre ou de marqueur/chronométreur<br />" +
                    "et de tenir le bar à tour de rôle<br />" +
                    "pendant les matchs de mon enfant ou de mon équipe.<br />"),
            entry(ACCORD_AUTORISATION_PARENTALE, " Vous venez d'inscrire votre enfant à une activité de l'ALOD.<br />" +
                    "Les parents doivent s'assurer de la présence de l'animateur avant de laisser leurs enfants devant ou dans la salle<br />" +
                    "et venir les reprendre à l'heure à l'issue de l'activité.<br />" +
                    "En dehors des heures d'activités, la responsabilité de l'ALOD n'est plus engagée.<br />" +
                    "Les animateurs de l'ALOD sont autorisés à faire pratiquer des soins d'urgence en cas de besoin<br />"),
            entry(ACCORD_PRISE_EN_CHARGE, "J'autorise l'animateur, entraineur ou administrateur de l'ALOD<br />" +
                    "à venir chercher votre enfant à l'école ou<br />\n" +
                    "au périscolaire de l'école OucheDinier afin de l'emmener à son cours<br />")


    );


}
