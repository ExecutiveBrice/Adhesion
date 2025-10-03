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

    public Seance addSeance(LocalDate dateSeance) {
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setDateSeance(dateSeance);
        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);

        return nouvelleSeance;
    }

    public Seance addFirstSeance(Activite activite) {
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setActivite(activite);

        LocalDate startingDate = LocalDate.of(2025, 9, 15);
        LocalDate prochaineSeance = startingDate.with(TemporalAdjusters.next(activite.getJour()));

        nouvelleSeance.setDateSeance(prochaineSeance);
        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);
        return nouvelleSeance;
    }


    public Seance addSeanceNextWeek(Activite activite) {
        Optional<Seance> lastSeance = activite.getSeances().stream().max(Comparator.comparing(Seance::getDateSeance));
        Seance nouvelleSeance = new Seance();
        nouvelleSeance.setActivite(activite);
        if(lastSeance.isPresent()){
            nouvelleSeance.setDateSeance(lastSeance.get().getDateSeance().plusWeeks(1));
        }


        nouvelleSeance.setEtatSeance(ESeance.PROGRAMMEE);

        return nouvelleSeance;
    }



    public void fillSeances(Activite activite, int nbWeeks) {
        activite.getSeances().add(addFirstSeance(activite));
        for (int i = 0; i < nbWeeks; i++) {
            activite.getSeances().add(addSeanceNextWeek(activite));
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
