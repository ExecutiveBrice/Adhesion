package com.wild.corp.adhesion.controllers;


import com.wild.corp.adhesion.models.Email;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class MailController {

    public static final Logger logger = LoggerFactory.getLogger(MailController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdherentServices adherentServices;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestBody Email mail) {
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
