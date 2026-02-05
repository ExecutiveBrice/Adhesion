package com.wild.corp.adhesion.services;

import brevo.ApiClient;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.*;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.models.resources.Groupe;
import com.wild.corp.adhesion.models.resources.Horaire;
import com.wild.corp.adhesion.repository.HistoriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Service
@Slf4j
public class EmailService {

    private Integer echecs;
    private Integer reussis;
    @Autowired
    private HistoriqueRepository historiqueRepository;

    @Autowired
    private AdherentServices adherentServices;

    @Autowired
    ParamTextServices paramTextServices;

    @Autowired
    private Environment environment;

    public Historique sendMessage(EmailContent mail) {
        Historique historique = null;
        List<String> listMail = new ArrayList<>();
        if(!mail.getDiffusion().isEmpty()) {
            adherentServices.findByGroup(mail.getDiffusion(), listMail);
        }else {
            listMail.addAll(mail.getDestinataires());
        }
        log.info("mailling {}",listMail);

        if (!listMail.isEmpty()) {
            echecs = 0;
            reussis = 0;
            listMail.forEach(email -> {
                if (patternMatches(email, "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")) {
                    singleMessage(listMail, mail,null, false);
                    reussis++;
                } else {
                    echecs++;
                    log.error("email invalide " + email);
                }
            });
            if(!mail.getDiffusion().isEmpty()) {
                List<String> resultats = mail.getDiffusion().stream()
                        .flatMap(groupe -> {
                            if (groupe.getChecked()) {
                                // Groupe coché → on retourne son nom
                                return Stream.of(groupe.getNom());
                            } else {
                                // Groupe non coché → on retourne les horaires cochés
                                return groupe.getHoraires().stream()
                                        .filter(Horaire::getChecked)
                                        .map(Horaire::getNom);
                            }
                        })
                        .toList();
                historique = new Historique(resultats.toString(), mail.getSubject(), echecs,reussis, LocalDateTime.now());
                historiqueRepository.save(historique);
            }

            log.info("nombre de mails envoyés " + reussis);

        }else{
            log.info("singleMessage");
            singleMessage(mail.getDestinataires(), mail, null, false);
        }

        return historique;
    }

    public void sendAutoMail(Adhesion adhesion, String sujetName, String corpsName, boolean attachement) {
        EmailContent mail = new EmailContent();
        mail.getDestinataires().add(Boolean.TRUE.equals(adhesion.getAdherent().getEmailRepresentant()) && adhesion.getAdherent().getRepresentant() != null ? adhesion.getAdherent().getRepresentant().getUser().getUsername() : adhesion.getAdherent().getUser().getUsername());
        String sujet = paramTextServices.getParamValue(sujetName);
        sujet = sujet.replaceAll("#activite#", adhesion.getActivite().getNom());
        mail.setSubject(sujet);

        String corps = paramTextServices.getParamValue(corpsName);
        corps = corps.replaceAll("#activite#", adhesion.getActivite().getNom());
        corps = corps.replaceAll("#prenom#", adhesion.getAdherent().getPrenom());
        corps = corps.replaceAll("#nom#", adhesion.getAdherent().getNom());
        mail.setText(corps);

        singleMessage(mail.getDestinataires(), mail, adhesion, attachement);
    }

    @Value("${image-storage-dir}")
    private Path imageStorageDir;



    public void singleMessage(List<String> destinataires, EmailContent mail, Adhesion adhesion, boolean attachement) {

        Properties prop = new Properties();
        prop.put("mail.debug", "false");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.host", "smtp-relay.brevo.com");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.trust", "smtp-relay.brevo.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


        ApiClient defaultClient = Configuration.getDefaultApiClient();
        // Configure API key authorization: api-key
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(System.getenv("BREVO_APIKEY_PRIVATE"));

        try {

            TransactionalEmailsApi api = new TransactionalEmailsApi();
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail("adhesion@alod.fr");
            sender.setName("ALOD");
            List<SendSmtpEmailTo> toList = new ArrayList<>();
            destinataires.forEach(destinataire -> {
                SendSmtpEmailTo to = new SendSmtpEmailTo();
                to.setEmail(destinataire);
                toList.add(to);
            });

            SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
            replyTo.setEmail("adhesion@alod.fr");
            replyTo.setName("ALOD");

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(sender);
            sendSmtpEmail.setTo(toList);

            String htmlTemplate = loadHtmlTemplateFromResources("templateALOD.html");
            htmlTemplate = htmlTemplate.replace("&&&TO_REPLACE&&&", mail.getText());
            sendSmtpEmail.setHtmlContent(htmlTemplate);

            sendSmtpEmail.setSubject(mail.getSubject());
            sendSmtpEmail.setReplyTo(replyTo);


            if (attachement) {
                List<SendSmtpEmailAttachment> attachmentList = new ArrayList<>();
                SendSmtpEmailAttachment attachment = new SendSmtpEmailAttachment();
                attachment.setName("Attestation_ALOD_" + adhesion.getAdherent().getPrenom() + "_" + adhesion.getAdherent().getNom() + ".pdf");

                Path prePath = this.imageStorageDir.resolve(String.valueOf(adhesion.getAdherent().getId()));
                if (!Files.exists(prePath)) {
                    Files.createDirectories(prePath);
                }

                final Path targetPath = prePath.resolve(attachment.getName());
                byte[] inFileBytes = Files.readAllBytes(targetPath);
                attachment.setContent(inFileBytes);
                attachmentList.add(attachment);
                sendSmtpEmail.setAttachment(attachmentList);
            }


            CreateSmtpEmail response = api.sendTransacEmail(sendSmtpEmail);
            log.info(response.toString());
        } catch (Exception e) {
            log.warn("Exception occurred:- " + e.getMessage());
        }

    }

    private String loadHtmlTemplateFromResources(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Fichier non trouvé dans les ressources : " + fileName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean patternMatches(String emailAddress, String regexPattern) {
        return Pattern.compile(regexPattern)
                .matcher(emailAddress)
                .matches();
    }

    public Historique diffusionTemplate(List<Groupe> maillingListe, Long templateId) {
        Historique historique = null;
        echecs = 0;
        reussis = 0;
        List<String> listMail = new ArrayList<>();
        adherentServices.findByGroup(maillingListe, listMail);

        if (!listMail.isEmpty() && templateId != 0) {
            listMail.forEach(email -> {

                if (patternMatches(email, "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")) {

                    log.info(email);
                    sendTemplate(email, templateId);
                    reussis++;
                } else {
                    echecs++;
                    log.error("email invalide " + email);
                }
            });

            List<String> resultats = maillingListe.stream()
                    .flatMap(groupe -> {
                        if (groupe.getChecked()) {
                            // Groupe coché → on retourne son nom
                            return Stream.of(groupe.getNom());
                        } else {
                            // Groupe non coché → on retourne les horaires cochés
                            return groupe.getHoraires().stream()
                                    .filter(Horaire::getChecked)
                                    .map(Horaire::getNom);
                        }
                    })
                    .toList();
            historique = new Historique(resultats.toString(), "Brevo Template n°"+templateId, echecs,reussis, LocalDateTime.now());

        }
        log.info("nombre de mails envoyés " + reussis);
        historiqueRepository.save(historique);
        return historique;
    }


    public void sendTemplate(String email, Long templateId) {

        Properties prop = new Properties();
        prop.put("mail.debug", "false");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.host", "smtp-relay.brevo.com");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.trust", "smtp-relay.brevo.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


        ApiClient defaultClient = Configuration.getDefaultApiClient();
        // Configure API key authorization: api-key
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(System.getenv("BREVO_APIKEY_PRIVATE"));

        try {

            TransactionalEmailsApi api = new TransactionalEmailsApi();
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail("communication@alod.fr");
            sender.setName("ALOD");
            List<SendSmtpEmailTo> toList = new ArrayList<>();
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(email);
            toList.add(to);

            SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
            replyTo.setEmail("communication@alod.fr");
            replyTo.setName("ALOD");

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(sender);
            sendSmtpEmail.setTo(toList);
            sendSmtpEmail.setReplyTo(replyTo);

            sendSmtpEmail.setTemplateId(templateId);
            log.info("getTemplateId " + sendSmtpEmail.getTemplateId());
            CreateSmtpEmail response = api.sendTransacEmail(sendSmtpEmail);
            log.info(response.toString());
        } catch (Exception e) {

            log.warn("Exception occurred: " + e.getMessage());
        }
    }

}
