package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.PresenceRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Service
@Transactional
public class PresenceServices {

    @Autowired
    PresenceRepository presenceRepository;




    public void fillPresences(Adhesion adhesion) {
        adhesion.getActivite().getSeances().forEach(seance -> {
            Presence presence = new Presence();
            presence.setAdhesion(adhesion);

            seance.getPresences().add(presence);
        });
    }

}
