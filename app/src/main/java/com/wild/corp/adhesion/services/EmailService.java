package com.wild.corp.adhesion.services;

import brevo.ApiClient;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.*;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;


@RequiredArgsConstructor
@Service
@Slf4j
public class EmailService {

    private Boolean inProgress = false;

    private Integer restant;

    @Autowired
    private AdherentServices adherentServices;

    @Autowired
    ParamTextServices paramTextServices;

    @Autowired
    private Environment environment;

    public void sendMessage(EmailContent mail) {
        inProgress = true;

        log.info("singleMessage");
        //mail.setDiffusion("brice_morel@hotmail.com");
        singleMessage(mail, null, false);

        restant = 0;
        Set<String> listMail = adherentServices.findByGroup(mail.getDiffusion());

        if (listMail != null ) {
            listMail.forEach(email -> {

                if (patternMatches(email, "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")) {

                    log.info(email);
                    singleMessage(new EmailContent(email, mail.getSubject(), mail.getText()),null, false);
                    restant++;
                } else {
                    log.error("email invalide " + email);
                }
            });
        }
        log.info("nombre de mails envoyés " + restant);
        inProgress = false;
    }

    public void sendAutoMail(Adhesion adhesion, String sujetName, String corpsName, boolean attachement) {
        EmailContent mail = new EmailContent();
        mail.setDiffusion(Boolean.TRUE.equals(adhesion.getAdherent().getEmailRepresentant()) && adhesion.getAdherent().getRepresentant() != null ? adhesion.getAdherent().getRepresentant().getUser().getUsername() : adhesion.getAdherent().getUser().getUsername());
        String sujet = paramTextServices.getParamValue(sujetName);
        sujet = sujet.replaceAll("#activite#", adhesion.getActivite().getNom());
        mail.setSubject(sujet);

        String corps = paramTextServices.getParamValue(corpsName);
        corps = corps.replaceAll("#activite#", adhesion.getActivite().getNom());
        corps = corps.replaceAll("#prenom#", adhesion.getAdherent().getPrenom());
        corps = corps.replaceAll("#nom#", adhesion.getAdherent().getNom());
        mail.setText(corps);

        singleMessage(mail, adhesion, attachement);
    }

    @Value("${image-storage-dir}")
    private Path imageStorageDir;

    public void singleMessageOld(EmailContent mail, Adhesion adhesion, boolean attachement) {

        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        final ClientOptions clientOptions = ClientOptions
                .builder()
                .apiKey(System.getenv("MJ_APIKEY_PUBLIC"))
                .apiSecretKey(System.getenv("MJ_APIKEY_PRIVATE"))
                .build();

        try {

            JSONObject email = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", System.getenv("MJ_MAIL")))
                    .put(Emailv31.Message.TO, new JSONArray()
                            .put(new JSONObject().put(Emailv31.Message.EMAIL, mail.getDiffusion()))
                    )
                    .put(Emailv31.Message.SUBJECT, mail.getSubject())
                    .put(Emailv31.Message.HTMLPART, mail.getText());

            if (attachement) {
                Path prePath = this.imageStorageDir.resolve(String.valueOf(adhesion.getAdherent().getId()));
                if (!Files.exists(prePath)) {
                    Files.createDirectories(prePath);
                }

                final Path targetPath = prePath.resolve("Attestation_ALOD_" + adhesion.getAdherent().getPrenom() + "_" + adhesion.getAdherent().getNom() + ".pdf");

                byte[] inFileBytes = Files.readAllBytes(targetPath);
                String encoded = Base64.getEncoder().encodeToString(inFileBytes);

                JSONArray fileAttachement = new JSONArray()
                        .put(new JSONObject()
                                .put("ContentType", "application/pdf")
                                .put("Filename", targetPath.getFileName())
                                .put("Base64Content", encoded));


                email.put(Emailv31.Message.ATTACHMENTS, fileAttachement);
            }

            client = new MailjetClient(clientOptions);
            request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(email));

            response = client.post(request);
            log.info(response.getRawResponseContent());
        } catch (IOException | MailjetException e) {
            log.error("create mail MessagingException " + e);
        }

        inProgress = false;
    }


    public void singleMessage(EmailContent mail, Adhesion adhesion, boolean attachement) {

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
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(mail.getDiffusion());

            toList.add(to);

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

    public void diffusionTemplate(String groupeName, Long templateId) {
        log.info(groupeName);
        inProgress = true;
        restant = 0;
        Set<String> listMail = adherentServices.findByGroup(groupeName);

        if (listMail != null && templateId != 0) {
            listMail.forEach(email -> {

                if (patternMatches(email, "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")) {

                    log.info(email);
                    sendTemplate(email, templateId);
                    restant++;
                } else {
                    log.error("email invalide " + email);
                }
            });
        }
        log.info("nombre de mails envoyés " + restant);
        inProgress = false;
    }

    public Boolean isInProgress() {
        return inProgress;
    }

    public Integer getRestant() {
        return restant;
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
            log.warn("Exception occurred:- " + e.getMessage());
        }
    }

}
