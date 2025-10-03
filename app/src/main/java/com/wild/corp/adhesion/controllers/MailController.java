package com.wild.corp.adhesion.controllers;


import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class MailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdherentServices adherentServices;


    @PostMapping(value = "/sendTemplate")
    public ResponseEntity<String> sendTemplate(@RequestParam(value = "groupeName") String groupeName, @RequestParam(value = "templateId") Long templateId) {
        log.debug("send sendTemplate " + templateId + " to " + groupeName);

        emailService.diffusionTemplate(groupeName, templateId);

        return new ResponseEntity<>(HttpStatus.OK);
    }


//    @GetMapping("/sendEmailTest")
//    public void sendEmailTest() {
//
//        Email email = new Email();
//
//        email.setFrom("@.com", "brice_morel@trial-.mlsender.net");
//        email.addRecipient("brice", "alod..@.com");
//
//
//        email.setSubject("Email subject");
//
//        email.setPlain("This is the text content");
//        email.setHtml("<p>This is the HTML content</p>");
//
//        MailerSend ms = new MailerSend();
//
//        ms.setToken("mlsn.");
//
//        try {
//            MailerSendResponse response = ms.emails().send(email);
//        } catch (MailerSendException e) {
//
//            e.printStackTrace();
//        }
//    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestBody EmailContent mail) {
        log.debug("send mail ");
        emailService.sendMessage(mail);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/isInProgress", method = RequestMethod.GET)
    public ResponseEntity<Boolean> isInProgress() {
        log.debug("isInProgress");
        return new ResponseEntity<>(emailService.isInProgress(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getRestant", method = RequestMethod.GET)
    public ResponseEntity<Integer> getRestant() {
        log.debug("getRestant");
        return new ResponseEntity<>(emailService.getRestant(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getTotal", method = RequestMethod.GET)
    public ResponseEntity<Integer> getTotal() {
        log.debug("getTotal");
        return new ResponseEntity<>(adherentServices.getAll().size(), HttpStatus.OK);
    }
}
