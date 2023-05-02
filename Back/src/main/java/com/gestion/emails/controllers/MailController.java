package com.gestion.emails.controllers;




import com.gestion.emails.models.Email;
import com.gestion.emails.services.EmailService;
import com.gestion.user.services.AdherentServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api_adhesion/email")
public class MailController {

    public static final Logger logger = LoggerFactory.getLogger(MailController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdherentServices adherentServices;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
    public ResponseEntity<String> send(@RequestBody Email mail) {
        logger.debug("send mail ");

        emailService.sendMessage(mail);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/isInProgress", method = RequestMethod.GET)
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isInProgress() {
        logger.debug("isInProgress");

        return new ResponseEntity<>(emailService.isInProgress(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getRestant", method = RequestMethod.GET)
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
    public ResponseEntity<Integer> getRestant() {
        logger.debug("getRestant");

        return new ResponseEntity<>(emailService.getRestant(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getTotal", method = RequestMethod.GET)
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMIN')")
    public ResponseEntity<Integer> getTotal() {
        logger.debug("getTotal");
        return new ResponseEntity<>(adherentServices.getAll().size(), HttpStatus.OK);
    }
}
