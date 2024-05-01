package com.wild.corp.adhesion.services;

import com.mailjet.client.errors.MailjetException;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.resource.Emailv31;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


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


//    @Async
//    public void sendEmail(SimpleMailMessage email) {
//
//        MimeMessage message = emailSender.createMimeMessage();
//
//        MimeMessageHelper helper = null;
//        try {
//            helper = new MimeMessageHelper(message, true);
//            helper.setTo(email.getTo());
//            helper.setSubject(email.getSubject());
//            helper.setText(email.getText(),true);
//        } catch (MessagingException e) {
//            e.printStackTrace();
//        }
//        log.debug("sendEmail getFrom "+email.getFrom());
//        log.debug("sendEmail getTo "+email.getTo());
//        log.debug("sendEmail getSubject "+email.getSubject());
//        log.debug("sendEmail getText "+email.getText());
//
//        try {
//            emailSender.send(message);
//        } catch (MailException e) {
//            log.debug("send mail " + e);
//        }
//    }
    public void sendMessage(Email mail) {
        inProgress = true;
        restant=0;

        if(mail.getDiffusion().contains("@")){
            log.info("singleMessage");
            singleMessage(mail, null, false);
        }else{
           // mailling(mail);
        }
    }

    public void sendAutoMail(Adhesion adhesion, String sujetName, String corpsName, boolean attachement){
        Email mail = new Email();
        mail.setDiffusion(adhesion.getAdherent().getRepresentant() != null ? adhesion.getAdherent().getRepresentant().getUser().getUsername():adhesion.getAdherent().getUser().getUsername());
        String sujet = paramTextServices.getParamValue(sujetName);
        sujet = sujet.replaceAll("#activite#",adhesion.getActivite().getNom());
        mail.setSubject(sujet);

        String corps = paramTextServices.getParamValue(corpsName);
        corps = corps.replaceAll("#activite#",adhesion.getActivite().getNom());
        corps = corps.replaceAll("#prenom#",adhesion.getAdherent().getPrenom());
        corps = corps.replaceAll("#nom#",adhesion.getAdherent().getNom());
        mail.setText(corps);

        singleMessage(mail, adhesion, attachement);
    }

    @Value("${image-storage-dir}")
    private Path imageStorageDir;
    public void singleMessage(Email mail, Adhesion adhesion, boolean attachement){

        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        final ClientOptions clientOptions = ClientOptions
                .builder()
                .apiKey(System.getenv("MJ_APIKEY_PUBLIC") )
                .apiSecretKey(System.getenv("MJ_APIKEY_PRIVATE") )
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

            if(attachement) {
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

        inProgress=false;
    }

//    public void mailling(Email mail){
//        MimeMessage message = emailSender.createMimeMessage();
//        String[][] listDiffusionByPack = sliceByPack(adherentServices.findByGroup(mail.getDiffusion()).stream().toList());
//
//        for (int i = 0; i < listDiffusionByPack.length; i++) {
//            System.out.println(Arrays.toString(listDiffusionByPack[i]));
//
//            MimeMessageHelper helper = null;
//            restant++;
//            try {
//                helper = new MimeMessageHelper(message, true);
//                helper.setBcc(listDiffusionByPack[i]);
//                helper.setSubject(mail.getSubject());
//                helper.setText(mail.getText(),true);
//            } catch (MessagingException e) {
//                log.error("create mail MessagingException " + e);
//            }
//            try {
//                emailSender.send(message);
//            } catch (MailException e) {
//                log.error("send mail MailException " + e);
//            }
//        }
//        inProgress=false;
//    }

    public Boolean isInProgress() {
        return inProgress;
    }

    public Integer getRestant() {
        return restant;
    }

    private String[][] sliceByPack(List<String> listDiffusion){
        int iteration = listDiffusion.size()/100+1;
        String [][] listDiffusionByPack = new String[iteration][100];
        int pas = 100;

        for (int i = 0; i < iteration; i++) {

            if(listDiffusion.size() < pas) {
                listDiffusionByPack[i] =listDiffusion.toArray(String[]::new);
            }else{
                listDiffusionByPack[i] = listDiffusion.subList(0, pas).toArray(String[]::new);
                listDiffusion = listDiffusion.subList(pas+1, listDiffusion.size());
            }
        }
        return listDiffusionByPack;
    }
}
