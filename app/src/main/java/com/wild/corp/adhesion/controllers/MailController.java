package com.wild.corp.adhesion.controllers;


import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.models.resources.Groupe;
import com.wild.corp.adhesion.repository.HistoriqueRepository;
import com.wild.corp.adhesion.services.AdherentServices;
import com.wild.corp.adhesion.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/email")
public class MailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdherentServices adherentServices;

    @Autowired
    HistoriqueRepository historiqueRepository;


    @GetMapping("/historique")
    public ResponseEntity<?> historique() {

        return ResponseEntity.ok(historiqueRepository.findAll());

    }

    @PostMapping(value = "/sendTemplate")
    public ResponseEntity<Historique> sendTemplate(@RequestBody List<Groupe> maillingListe, @RequestParam(value = "templateId") Long templateId) {
        log.debug("send sendTemplate " + templateId);

        Historique historique =  emailService.diffusionTemplate(maillingListe, templateId);

        return new ResponseEntity<>(historique, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Historique> send(@RequestBody EmailContent mail) {
        log.debug("send mail ");
        Historique historique = emailService.sendMessage(mail);
        return new ResponseEntity<>(historique, HttpStatus.OK);
    }

}
