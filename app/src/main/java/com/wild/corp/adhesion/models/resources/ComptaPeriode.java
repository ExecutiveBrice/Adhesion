package com.wild.corp.adhesion.models.resources;

import com.wild.corp.adhesion.models.ComptaActivite;

import java.time.LocalDate;
import java.util.List;

public record ComptaPeriode(List<ComptaActivite> recap, List<AdhesionCompta> adhesions) {
    public record PaiementCompta(Long id, LocalDate dateReglement, String typeReglement, Integer montant,
                                Boolean inclusDansPeriode) {
        public PaiementCompta {
            inclusDansPeriode = Boolean.TRUE.equals(inclusDansPeriode);
        }

        public PaiementCompta(Long id, LocalDate dateReglement, String typeReglement, Integer montant) {
            this(id, dateReglement, typeReglement, montant, false);
        }
    }

    public record AdhesionCompta(Long id, String nomActivite, String nomAdherent, String prenomAdherent, String emailAdherent,
                                String statutActuel, Boolean rapprochement, List<PaiementCompta> paiements, Integer totalPeriode) { }
}
