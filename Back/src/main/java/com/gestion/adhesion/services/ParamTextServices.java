package com.gestion.adhesion.services;

import com.gestion.adhesion.models.ParamText;
import com.gestion.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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


        if(!paramTextRepository.existsByParamName("Sujet_Mail_Annulation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Sujet_Mail_Annulation")
                    .paramValue("Annulation de votre demande d'adhésion #activite#").build());
        }
        if(!paramTextRepository.existsByParamName("Corp_Mail_Annulation")) {
            paramTextRepository.save(ParamText.builder()
                    .paramName("Corp_Mail_Annulation")
                    .paramValue("Bonjour #prenom# #nom#,<br />" +
                            "Votre adhésion est restée incomplète pendant plus de 8 jours,<br />" +
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
