package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.SeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
public class SeanceServices {

    @Autowired
    SeanceRepository seanceRepository;

    public Seance addFirstSeance(Activite activite) {
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setActivite(activite);


        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);
        return nouvelleSeance;
    }





    public void fillSeances(Activite activite, int nbWeeks) {
        activite.getSeances().add(addFirstSeance(activite));
        for (int i = 0; i < nbWeeks; i++) {

        }
    }

    public void modifyDay(Activite activite){
        List<Seance> seances  = activite.getSeances().stream().filter(seance -> seance.getEtatSeance().equals(ESeance.PROGRAMMEE)).toList();
        int nbSeancesRestantes = seances.size();
        seances.forEach(seance -> {
            seanceRepository.delete(seance);
        });
        fillSeances(activite, nbSeancesRestantes);
    }
}
