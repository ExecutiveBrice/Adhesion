package com.gestion.emails.services;



import com.gestion.emails.models.Email;
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


@RequiredArgsConstructor
@Service
    public class EmailService {
    public static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private Boolean inProgress = false;

    private Integer restant;

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
        MimeMessage message = emailSender.createMimeMessage();

        MailingList.getInstance().getListing().forEach(strings -> strings.forEach(s -> {
            MimeMessageHelper helper = null;
            restant++;
            try {
                helper = new MimeMessageHelper(message, true);
                helper.setTo(s);
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
        }));

        inProgress=false;
    }

    public Boolean isInProgress() {
        return inProgress;
    }

    public Integer getRestant() {
        return restant;
    }
}
