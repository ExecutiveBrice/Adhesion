package com.wild.corp.adhesion.models.resources;

import java.util.List;

public record CalendrierGoogleResponse(
        List<EvenementGoogleAgendaResponse> evenements,
        List<String> erreurs
) {
}
