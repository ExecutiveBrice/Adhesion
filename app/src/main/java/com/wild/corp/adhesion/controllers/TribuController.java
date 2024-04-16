package com.wild.corp.adhesion.controllers;

import com.wild.corp.adhesion.models.ActiviteNm1;
import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.services.TribuServices;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/tribu")
public class TribuController {

    @Autowired
    TribuServices tribuServices;

    @GetMapping("/getConnectedTribu")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getConnetedTribu(Authentication principal) {
        Tribu tribu = tribuServices.getConnetedTribu(principal.getName());
        return ResponseEntity.ok(tribu);
    }

    @PostMapping("/addActivitesNm1")
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('MODERATOR') or hasRole('BUREAU') or hasRole('ADMINISTRATEUR') or hasRole('ADMIN')")
    public ResponseEntity<?> addActivitesNm1(@PathParam("tribuUuid") String tribuUuid, @RequestBody List<ActiviteNm1> activitesNm1) {
        return ResponseEntity.ok(tribuServices.addActivitesNm1(UUID.fromString(tribuUuid), activitesNm1));
    }

    @GetMapping("/getTribuByUuid")
    @PreAuthorize("hasRole('SECRETAIRE') or hasRole('ADMINISTRATEUR')")
    public ResponseEntity<?> getTribuByUuid(@PathParam("tribuUuid") String tribuUuid) {

        Tribu tribu = tribuServices.getTribuByUuid(UUID.fromString(tribuUuid));
        return ResponseEntity.ok(tribu);
    }

}
