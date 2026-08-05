package com.wild.corp.adhesion.models.resources;

import java.time.LocalDateTime;

public record EvenementGoogleAgendaResponse(
        String id,
        String titre,
        String lieu,
        String commentaire,
        LocalDateTime debut,
        LocalDateTime fin,
        boolean journeeEntiere,
        String agenda,
        String agendaSource
) {
}
