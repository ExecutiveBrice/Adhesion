package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.ComptaActivite;
import com.wild.corp.adhesion.models.Paiement;
import com.wild.corp.adhesion.models.resources.ComptaPeriode;
import com.wild.corp.adhesion.models.resources.ComptaPeriode.AdhesionCompta;
import com.wild.corp.adhesion.models.resources.ComptaPeriode.PaiementCompta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

@Service
public class ComptaService {

    @Autowired
    ActiviteServices activiteServices;

    @Autowired
    AdhesionServices adhesionServices;

    @Autowired
    AdhesionRepository adhesionRepository;

    private static final Set<String> TYPES_PAIEMENT = Set.of("HelloAsso", "HelloAsso 3X", "Chèque",
            "Chèque 3X", "Espèces", "Pass sport", "Intermarché", "Autre", "CB");

    public List<ComptaActivite> getAll(LocalDate dateDebut, LocalDate dateFin) {
        return getPeriode(dateDebut, dateFin).recap();
    }

    public ComptaPeriode getPeriode(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException("La période comptable doit avoir une date de début antérieure ou égale à la date de fin.");
        }

        Map<String, ComptaActivite> comptes = new LinkedHashMap<>();
        activiteServices.getAll().forEach(activite -> {
            if (activite.getNom() != null) {
                comptes.computeIfAbsent(activite.getNom(), nom -> {
                    ComptaActivite compte = new ComptaActivite();
                    compte.setNomActivite(nom);
                    return compte;
                });
            }
        });

        List<AdhesionCompta> details = new ArrayList<>();
        // Seules les adhésions contribuant à la période sont retenues, avec tous leurs règlements.
        for (Adhesion adhesion : adhesionServices.getAll()) {
            if (!Boolean.TRUE.equals(adhesion.getValidPaiementSecretariat())
                    || adhesion.getActivite() == null || adhesion.getPaiements() == null) {
                continue;
            }
            ComptaActivite compte = comptes.get(adhesion.getActivite().getNom());
            if (compte == null) {
                continue;
            }
            List<PaiementCompta> tousLesPaiements = new ArrayList<>();
            boolean adhesionRetenue = false;
            int totalPeriode = 0;
            for (Paiement paiement : adhesion.getPaiements()) {
                if (paiement == null) {
                    continue;
                }
                LocalDate date = paiement.getDateReglement();
                // Les deux dates sont incluses, y compris pour une seule journée.
                boolean inclus = date != null && paiement.getMontant() != null
                        && paiement.getTypeReglement() != null && TYPES_PAIEMENT.contains(paiement.getTypeReglement())
                        && !date.isBefore(dateDebut) && !date.isAfter(dateFin);
                tousLesPaiements.add(new PaiementCompta(paiement.getId(), date,
                        paiement.getTypeReglement(), paiement.getMontant(), inclus));
                if (!inclus) {
                    continue;
                }
                int montant = paiement.getMontant();
                switch (paiement.getTypeReglement()) {
                    case "HelloAsso" -> compte.setHelloAsso(Math.addExact(compte.getHelloAsso(), montant));
                    case "HelloAsso 3X" -> compte.setHelloAsso3x(Math.addExact(compte.getHelloAsso3x(), montant));
                    case "Chèque" -> compte.setCheque(Math.addExact(compte.getCheque(), montant));
                    case "Chèque 3X" -> compte.setCheque3x(Math.addExact(compte.getCheque3x(), montant));
                    case "Espèces" -> compte.setEspece(Math.addExact(compte.getEspece(), montant));
                    case "Pass sport" -> compte.setPassport(Math.addExact(compte.getPassport(), montant));
                    case "Intermarché" -> compte.setIntermarche(Math.addExact(compte.getIntermarche(), montant));
                    case "CB" -> compte.setCb(Math.addExact(compte.getCb(), montant));
                    case "Autre" -> compte.setAutre(Math.addExact(compte.getAutre(), montant));
                    default -> { continue; }
                }
                adhesionRetenue = true;
                totalPeriode = Math.addExact(totalPeriode, montant);
            }
            if (adhesionRetenue) {
                tousLesPaiements.sort(Comparator.comparing(PaiementCompta::dateReglement, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(PaiementCompta::id, Comparator.nullsLast(Long::compareTo)));
                details.add(new AdhesionCompta(adhesion.getId(), compte.getNomActivite(),
                        adhesion.getAdherent() == null ? null : adhesion.getAdherent().getNom(),
                        adhesion.getAdherent() == null ? null : adhesion.getAdherent().getPrenom(),
                        emailContact(adhesion.getAdherent()),
                        adhesion.getStatutActuel(), adhesion.getRapprochement(), List.copyOf(tousLesPaiements), totalPeriode));
            }
        }
        details.sort(Comparator.comparing(AdhesionCompta::nomActivite)
                .thenComparing(AdhesionCompta::nomAdherent, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdhesionCompta::prenomAdherent, Comparator.nullsLast(String::compareTo))
                .thenComparing(AdhesionCompta::id, Comparator.nullsLast(Long::compareTo)));
        return new ComptaPeriode(new ArrayList<>(comptes.values()), details);
    }

    private String emailContact(Adherent adherent) {
        if (adherent == null) {
            return null;
        }
        Adherent contact = Boolean.TRUE.equals(adherent.getEmailRepresentant()) && adherent.getRepresentant() != null
                ? adherent.getRepresentant() : adherent;
        return contact.getUser() == null ? null : contact.getUser().getUsername();
    }

    @Transactional
    public void updateRapprochement(Long adhesionId, boolean rapprochement, String utilisateur) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhésion introuvable."));
        adhesion.setRapprochement(rapprochement);
        adhesionRepository.save(adhesion);
        adhesionServices.addModification(utilisateur, adhesionId, "Rapprochement depuis la comptabilité : "
                + (rapprochement ? "Oui" : "Non"));
    }

    @Transactional
    public void savePaiement(Long adhesionId, PaiementCompta demande, String utilisateur) {
        if (demande.montant() == null || demande.dateReglement() == null
                || demande.typeReglement() == null || !TYPES_PAIEMENT.contains(demande.typeReglement())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant, la date et le moyen de paiement sont obligatoires.");
        }
        Adhesion adhesion = adhesionRepository.findById(adhesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhésion introuvable."));
        Paiement paiement;
        if (demande.id() == null) {
            paiement = new Paiement();
            adhesion.getPaiements().add(paiement);
        } else {
            paiement = adhesion.getPaiements().stream().filter(p -> Objects.equals(p.getId(), demande.id()))
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Ce règlement n’appartient pas à cette adhésion."));
        }
        paiement.setMontant(demande.montant());
        paiement.setDateReglement(demande.dateReglement());
        paiement.setTypeReglement(demande.typeReglement());
        adhesionRepository.save(adhesion);
        adhesionServices.addModification(utilisateur, adhesionId, "Enregistrement d’un règlement depuis la comptabilité : "
                + demande.montant() + " / " + demande.typeReglement() + " / " + demande.dateReglement());
    }

    @Transactional
    public void deletePaiement(Long adhesionId, Long paiementId, String utilisateur) {
        Adhesion adhesion = adhesionRepository.findById(adhesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhésion introuvable."));
        Paiement paiement = adhesion.getPaiements().stream().filter(p -> Objects.equals(p.getId(), paiementId))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ce règlement n’appartient pas à cette adhésion."));
        adhesion.getPaiements().remove(paiement);
        adhesionRepository.save(adhesion);
        adhesionServices.addModification(utilisateur, adhesionId, "Suppression d’un règlement depuis la comptabilité : " + paiementId);
    }
}
