package com.gestion.emails.services;


import com.gestion.emails.models.Email;
import com.gestion.user.services.AdherentServices;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;


@RequiredArgsConstructor
@Service
    public class EmailService {
    public static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private Boolean inProgress = false;

    private Integer restant;

    @Autowired
    private AdherentServices adherentServices;
    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private Environment environment;

    @Async
    public void sendEmail(SimpleMailMessage email) {


        MimeMessage message = emailSender.createMimeMessage();

        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setText(email.getText(),true);


        } catch (MessagingException e) {
            e.printStackTrace();
        }
        logger.debug("sendEmail getFrom "+email.getFrom());
        logger.debug("sendEmail getFrom "+email.getTo());
        logger.debug("sendEmail getFrom "+email.getSubject());
        logger.debug("sendEmail getText "+email.getText());

        try {
            emailSender.send(message);
        } catch (MailException e) {
            logger.debug("send mail " + e);
        }
    }
    public void sendMessage(Email mail) {
        inProgress = true;
        restant=0;


        if("@".contains(mail.getDiffusion())){
            singleMessage(mail);
        }else{
            mailling(mail);
        }

    }

    public void singleMessage(Email mail){
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);
            helper.setTo(mail.getDiffusion());
            helper.setSubject(mail.getSubject());
            helper.setText(mail.getText(),true);
        } catch (MessagingException e) {
            logger.error("create mail MessagingException " + e);
        }
        try {
            emailSender.send(message);
        } catch (MailException e) {
            logger.error("send mail MailException " + e);
        }
        inProgress=false;
    }
    public void mailling(Email mail){
        MimeMessage message = emailSender.createMimeMessage();
        String[][] listDiffusionByPack = sliceByPack(adherentServices.findByGroup(mail.getDiffusion()));

        for (int i = 0; i < listDiffusionByPack.length; i++) {

            MimeMessageHelper helper = null;
            restant++;
            try {
                helper = new MimeMessageHelper(message, true);
                helper.setBcc(listDiffusionByPack[i]);
                helper.setSubject(mail.getSubject());
                helper.setText(mail.getText(),true);
            } catch (MessagingException e) {
                logger.error("create mail MessagingException " + e);
            }

            try {
                emailSender.send(message);
            } catch (MailException e) {
                logger.error("send mail MailException " + e);
            }

        }
        inProgress=false;
    }

    public Boolean isInProgress() {
        return inProgress;
    }

    public Integer getRestant() {
        return restant;
    }

    private String[][] sliceByPack(List<String> listDiffusion){
        int iteration = listDiffusion.size()/100+1;
        System.out.println(listDiffusion.toString());
        String [][] listDiffusionByPack = new String[iteration][100];
        int pas = 100;


        for (int i = 0; i < iteration; i++) {
            System.out.println(listDiffusion.size());
            System.out.println(i*pas+pas);
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
