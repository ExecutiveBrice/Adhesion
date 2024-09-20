package com.wild.corp.adhesion.controllers;


import com.mailersend.sdk.emails.Email;

import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.exceptions.MailerSendException;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class MailController {

    public static final Logger logger = LoggerFactory.getLogger(MailController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdherentServices adherentServices;

    @GetMapping("/sendEmailTest")
    public void sendEmailTest() {

        Email email = new Email();

        email.setFrom("brice_morel@hotmail.com", "brice_morel@trial-7dnvo4dz0dxl5r86.mlsender.net");
        email.addRecipient("brice", "alod.section.fete@gmail.com");


        email.setSubject("Email subject");

        email.setPlain("This is the text content");
        email.setHtml("<p>This is the HTML content</p>");

        MailerSend ms = new MailerSend();

        ms.setToken("mlsn.e1ae68be56b3e19e9321927d35782861ac1cae949647c2f3a4b54990d3141697");

        try {
            MailerSendResponse response = ms.emails().send(email);
        } catch (MailerSendException e) {

            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestBody EmailContent mail) {
        logger.debug("send mail ");

        emailService.sendMessage(mail);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/isInProgress", method = RequestMethod.GET)
    public ResponseEntity<Boolean> isInProgress() {
        logger.debug("isInProgress");

        return new ResponseEntity<>(emailService.isInProgress(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getRestant", method = RequestMethod.GET)
    public ResponseEntity<Integer> getRestant() {
        logger.debug("getRestant");

        return new ResponseEntity<>(emailService.getRestant(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getTotal", method = RequestMethod.GET)
    public ResponseEntity<Integer> getTotal() {
        logger.debug("getTotal");
        return new ResponseEntity<>(adherentServices.getAll().size(), HttpStatus.OK);
    }
}
