package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ESeance;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MiseAJourSeanceRequest(
        ESeance etatSeance,
        @Size(max = 255) String commentaire,
        Boolean commentairePresent,
        LocalDate date,
        LocalTime heureDebut,
        Boolean horairePresent,
        Long salleId,
        Boolean sallePresente,
        List<@NotNull @Valid ResponsabiliteSeanceRequest> responsabilites
) {
    public MiseAJourSeanceRequest(ESeance etatSeance, String commentaire, Boolean commentairePresent,
                                  LocalDate date, LocalTime heureDebut, Boolean horairePresent,
                                  Long salleId, Boolean sallePresente) {
        this(etatSeance, commentaire, commentairePresent, date, heureDebut, horairePresent,
                salleId, sallePresente, null);
    }
}
