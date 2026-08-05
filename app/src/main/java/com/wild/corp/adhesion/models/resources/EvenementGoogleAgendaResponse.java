package com.wild.corp.adhesion.models.resources;

import java.time.LocalDateTime;

public record EvenementGoogleAgendaResponse(
        String id,
        String titre,
        String lieu,
        LocalDateTime debut,
        LocalDateTime fin,
        boolean journeeEntiere,
        String agenda
) {
}
