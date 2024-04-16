package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ParamText;
import com.wild.corp.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.wild.corp.adhesion.services.AccordServices.*;


@Service
public class ParamTextServices {

    @Autowired
    ParamTextRepository paramTextRepository;



    public List<ParamText> getAll (){
        List<ParamText> params = paramTextRepository.findAll();
        return params;
    }

    public ParamText findByParamName(String paramName){
        return paramTextRepository.findByParamName(paramName).get();
    }

    public String getParamValue(String paramName){
        return paramTextRepository.findByParamName(paramName).get().getParamValue();
    }
    public ParamText save(ParamText param){

        if(paramTextRepository.existsByParamName(param.getParamName())){
            ParamText dbparam = findByParamName(param.getParamName());
            dbparam.setParamValue(param.getParamValue());
            return paramTextRepository.save(dbparam);
        }
        return paramTextRepository.save(param);
    }


    public void fillParamText(){

        if(!paramTextRepository.existsByParamName(REGLEMENT_INTERIEUR)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(REGLEMENT_INTERIEUR)
                    .paramValue(" Vous vous inscrivez à une activité au sein de l’ALOD (Amicale Laïque de L’Ouche Dinier). <br />" +
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
                            "Une section ne peut s’ouvrir que sur décision du Conseil d’Administration.<br />").build());
        }

        if(!paramTextRepository.existsByParamName(ATTESTATION_SANTE)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(ATTESTATION_SANTE)
                    .paramValue("Le certificat médical n'est obligatoire<br />" +
                            "que dans le cas d'une réponse positive au questionnaire de santé<br />" +
                            "<a href=\"https://www.alod.fr/wp-content/uploads/2022/04/INSCRIPTION-Questionnaire_de_sante.pdf\" target=\"_blank\">Questionnaire de santé</a><br>" +
                            "Si vous avez répondu Non à toutes les questions du questionnaire,<br>" +
                            "vous pouvez valider cet accord d'attestation de votre bonne santé.<br>" +
                            "Sinon refusez le pour signifier au secrétariat que vous devrez fournir un certificat médical<br>").build());
        }

        if(!paramTextRepository.existsByParamName(VIE_CLUB)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(VIE_CLUB)
                    .paramValue("J'accepte également de participer,<br />" +
                            "dans la limite de mes possibilités et de manière collective,<br />" +
                            "aux déplacements de l'équipe,<br />" +
                            "aux responsabilités d'arbitre ou de marqueur/chronométreur<br />" +
                            "et de tenir le bar à tour de rôle<br />" +
                            "pendant les matchs de mon enfant ou de mon équipe.<br />").build());
        }

        if(!paramTextRepository.existsByParamName(AUTORISATION_PARENTALE)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(AUTORISATION_PARENTALE)
                    .paramValue(" Vous venez d'inscrire votre enfant à une activité de l'ALOD.<br />" +
                            "Les parents doivent s'assurer de la présence de l'animateur avant de laisser leurs enfants devant ou dans la salle<br />" +
                            "et venir les reprendre à l'heure à l'issue de l'activité.<br />" +
                            "En dehors des heures d'activités, la responsabilité de l'ALOD n'est plus engagée.<br />" +
                            "Les animateurs de l'ALOD sont autorisés à faire pratiquer des soins d'urgence en cas de besoin<br />").build());
        }

        if(!paramTextRepository.existsByParamName(PRISE_EN_CHARGE)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(PRISE_EN_CHARGE)
                    .paramValue("J'autorise l'animateur, entraineur ou administrateur de l'ALOD<br />" +
                            "à venir chercher votre enfant à l'école ou<br />" +
                            "au périscolaire de l'école OucheDinier afin de l'emmener à son cours<br />").build());
        }

        if(!paramTextRepository.existsByParamName(RGPD)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(RGPD)
                    .paramValue("Les informations recueillies sur ce formulaire sont enregistrées dans un fichier informatisé par le secrétariat de l'ALOD pour permettre le fonctionnement des activités. La base légale du traitement est le contrat entre l'ALOD et le futur adhérent/liciencié.<br />" +
                            "Les données collectées seront communiquées aux seuls destinataires suivants : secrétariat de l'ALOD de l'UFOLEP et de la FAL.<br />" +
                            "Les données sont conservées pendant toute la durée des activitées aux quelles l'adhérent/licencié à souscrit au sein de l'ALOD suivi d'une année d'historisation.<br />" +
                            "Vous pouvez accéder aux données vous concernant, les rectifier, demander leur effacement ou exercer votre droit à la limitation du traitement de vos données.<br />" +
                            "Consultez le site <a href=\"https://www.cnil.fr\" target=\"_blank\">www.cnil.fr</a> pour plus d’informations sur vos droits.<br />" +
                            "Pour exercer ces droits ou pour toute question sur le traitement de vos données dans ce dispositif, vous pouvez contacter (le cas échéant, notre délégué à la protection des données ou le service chargé de l’exercice de ces droits) : alod.amicale@gmail.com <br />" +
                            "Si vous estimez, après nous avoir contactés, que vos droits « Informatique et Libertés » ne sont pas respectés, vous pouvez adresser une réclamation à la CNIL.<br />").build());
        }

        if(!paramTextRepository.existsByParamName(DROIT_IMAGE)) {
            paramTextRepository.save(ParamText.builder()
                    .paramName(DROIT_IMAGE)
                    .paramValue("J'autorise l'ALOD à utiliser les photographies, films, travaux et productions réalisés dans le cadre de l'activité sportive.<br />"+
                            "Nous utiliserons les photos et autres médias pris durant les cours pour la promotion de l'ALOD (site internet, affiches, flyer, forum des associations)<br />"+
                    "<b>Pour les adhérents du Basket, l'acceptation du droit à l'image est compris dans l'adhésion à la FFBB.").build());
        }

        if(!paramTextRepository.existsByParamName("Text_Maintenance")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Text_Maintenance")
                    .paramValue("<strong>Site en maintenance</strong><br />" +
                            "Veuillez nous excuser pour la gène occasionnée").build());
        }

        if(!paramTextRepository.existsByParamName("Text_Accueil")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Text_Accueil")
                    .paramValue("Bonjour,<br />Bienvenue sur le site des adhésions de l'ALOD<br />" +
                            "Les préinscriptions sont possibles jusqu'au 27 Mai<br />" +
                            "Les inscriptions débuteront à partir du 4 Juin").build());
        }

        if(!paramTextRepository.existsByParamName("Text_Inscription")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Text_Inscription")
                    .paramValue("<b>Bienvenue sur le site d'inscription de l'ALOD</b><br />" +
                            "Vous n'avez, dans un premier temps,<br />" +
                            "qu'à saisir votre email et un mot de passe.<br />&nbsp;<br />&nbsp;<br />" +
                            "Il est également nécéssaire d'accepter<br />" +
                            "les conditions d'utilisation des données personnelles<br />" +
                            "avant d'aller plus loin dans les inscriptions.").build());
        }
        if(!paramTextRepository.existsByParamName("Text_Contact")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Text_Contact")
                    .paramValue("Vous rencontrez un problème ? <br />" +
                            "Vous ne comprenez pas comment cela fonctionne ? <br />" +
                            "Vous avez une remarque ou une idée d'amélioration ? <br />&nbsp;<br /><br />&nbsp;<br />" +
                            "<b>N'hésitez pas à nous envoyer un mail:</b><br />" +
                            "Donnez nous le maximum d'informations pour nous aider à vous aider<br />" +
                            "<i></b><font color=\"red\">Surtout n'oubliez pas votre contact pour que nous puissions vous répondre!</font></b><br />(e-mail, téléphone, adresse, pigeon... ;)</i>").build());
        }
        if(!paramTextRepository.existsByParamName("Text_MonAdhesion")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Text_MonAdhesion")
                    .paramValue("").build());
        }

        if(!paramTextRepository.existsByParamName("Sujet_Mail_Liste_Attente")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Liste_Attente")
                    .paramValue("Demande d'adhésion #activite# incomplète").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Liste_Attente")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Liste_Attente")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "l'activité #activite# est complète pour le moment.<br />" +
                            "<br />" +
                            "Et effectuer le paiement, soit en ligne sur HelloAsso, soit au secrétariat.<br />" +
                            "Le certificat médicale, si il est nécéssaire pour cette activitée, ne vous sera demandé qu'au démarrage de l'activité.<br /><br />" +
                            "Compte tenu du nombre important de demandes d'adhésions, nous nous réservons le droit d'annuler votre demande si elle n'est pas completée dans les 5 jours.<br /><br />" +

                            "Cordialement,<br />" +
                            "Le secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }



        if(!paramTextRepository.existsByParamName("Sujet_Mail_Rappel")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Rappel")
                    .paramValue("Demande d'adhésion #activite# incomplète").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Rappel")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Rappel")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre demande d'adhesion à #activite# est incomplète.<br />" +
                            "Pour valider une adhésion il faut au minimum accepter les accords sur l'application<br />" +
                            "Et effectuer le paiement, soit en ligne sur HelloAsso, soit au secrétariat.<br />" +
                            "Le certificat médicale, si il est nécéssaire pour cette activitée, ne vous sera demandé qu'au démarrage de l'activité.<br /><br />" +
                            "Compte tenu du nombre important de demandes d'adhésions, nous nous réservons le droit d'annuler votre demande si elle n'est pas completée dans les 5 jours.<br /><br />" +

                            "Cordialement,<br />" +
                            "Le secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }


        if(!paramTextRepository.existsByParamName("Sujet_Mail_Annulation_Manuelle")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Annulation_Manuelle")
                    .paramValue("Annulation de votre demande d'adhésion #activite#").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Annulation_Manuelle")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Annulation_Manuelle")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre demande d'adhésion a été annulée par le secrétariat.<br />" +
                            "Vous pouvez demander des informations supplémentaires au secrétariat sur les raisons de cette annulation.<br /><br />" +
                            "Cordialement,<br />" +
                            "Secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }


        if(!paramTextRepository.existsByParamName("Sujet_Mail_Annulation_Automatique")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Annulation_Automatique")
                    .paramValue("Annulation de votre demande d'adhésion #activite#").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Annulation_Automatique")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Annulation_Automatique")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre adhésion est restée incomplète pendant plus de 10 jours,<br />" +
                            "Nous avons donc annulée votre demande pour libérer une place.<br />" +
                            "Si il reste des places disponible, vous pourrez la réactivée en contactant le secrétariat pour vous permettre de la completer.<br /><br />" +

                            "Cordialement,<br />" +
                            "Secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }


        if(!paramTextRepository.existsByParamName("Sujet_Mail_Reactivation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Reactivation")
                    .paramValue("Votre demande d'adhésion #activite# a changé de statut").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Reactivation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Reactivation")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre adhésion #activite# à changée de statut,<br />" +
                            "Vous pouvez vous rendre sur l'application <a href='alod.fr/adhesion'>alod.fr/adhesion</a> pour valider votre adhesion<br /><br />" +

                            "Cordialement,<br />" +
                            "Secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }


        if(!paramTextRepository.existsByParamName("Sujet_Mail_Validation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Validation")
                    .paramValue("Votre demande d'adhésion #activite# est maintenant validée").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Validation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Validation")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre adhésion #activite# est maintenant validée,<br /><br />" +
                            "Cordialement,<br />" +
                            "Secretariat de l'ALOD<br />" +
                            "<a href='alod.fr'>alod.fr</a>").build());
        }

    }

}
