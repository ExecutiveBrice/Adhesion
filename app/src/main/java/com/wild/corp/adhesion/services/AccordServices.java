package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.repository.AccordRepository;
import com.wild.corp.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Service
public class AccordServices {

    public static final String REGLEMENT_INTERIEUR = "ReglementInterieur";
    public static final String ATTESTATION_SANTE = "AttestationSante";
    public static final String VIE_CLUB = "VieClub";
    public static final String AUTORISATION_PARENTALE = "AutorisationParentale";
    public static final String PRISE_EN_CHARGE = "PriseEnCharge";
    public static final String RGPD = "RGPD";
    public static final String DROIT_IMAGE = "DroitImage";

    public static final Map<String, Map<String, String>> ACCORDS = Map.ofEntries(
            entry(REGLEMENT_INTERIEUR, Map.ofEntries(entry("title", "Règlement intérieur"), entry("bouton", "au règlement intérieur"), entry("refusable", "false"))),
            entry(ATTESTATION_SANTE, Map.ofEntries(entry("title", "Attestation santé"), entry("bouton", "d'attestation de santé"), entry("refusable", "true"))),
            entry(VIE_CLUB, Map.ofEntries(entry("title", "Vie du club"), entry("bouton", "à la vie du club"), entry("refusable", "true"))),
            entry(AUTORISATION_PARENTALE, Map.ofEntries(entry("title", "Autorisation Parentale"), entry("bouton", "d'autorisation parentale"), entry("refusable", "true"))),
            entry(PRISE_EN_CHARGE, Map.ofEntries(entry("title", "Prise en charge"), entry("bouton", "d'attestation de prise en charge"), entry("refusable", "true"))),
            entry(RGPD, Map.ofEntries(entry("title", "Autorisation d'utilisation des données personnelles"), entry("bouton", " à l'utilisation de vos données personnelles"), entry("refusable", "false"))),
            entry(DROIT_IMAGE, Map.ofEntries(entry("title", "Droit à l'image"), entry("bouton", " a l'utilisation de votre image"), entry("refusable", "true")))
    );

    @Autowired
    AccordRepository accordRepository;

    @Autowired
    ParamTextRepository paramTextRepository;

    public Accord createAccord(String accordKey) {
        String paramText = paramTextRepository.findByParamName(accordKey).get().getParamValue();
        Accord newAccord = new Accord(accordKey, ACCORDS.get(accordKey).get("title"), ACCORDS.get(accordKey).get("bouton"), Boolean.parseBoolean(ACCORDS.get(accordKey).get("refusable")), paramText);
        return accordRepository.save(newAccord);
    }

    public void refreshAccords() {
        List<Accord> accords = accordRepository.findAll();
        accords.forEach(accord -> {
            String nomAccord = accord.getNom();
            if ("Reglement Interieur".equals(nomAccord)) {
                nomAccord = "ReglementInterieur";
            }
            if ("Droit Image".equals(nomAccord)) {
                nomAccord = "DroitImage";
            }
            if ("Attestation Sante".equals(nomAccord)) {
                nomAccord = "AttestationSante";
            }
            if ("Autorisation Parentale".equals(nomAccord)) {
                nomAccord = "AutorisationParentale";
            }
            if ("Prise en Charge".equals(nomAccord)) {
                nomAccord = "PriseEnCharge";
            }
            if ("Vie du Club".equals(nomAccord)) {
                nomAccord = "VieClub";
            }


            accord.setNom(nomAccord);
            accord.setText(paramTextRepository.findByParamName(nomAccord).get().getParamValue());
            accord.setRefusable(Boolean.parseBoolean(ACCORDS.get(nomAccord).get("refusable")));
            accord.setBouton(ACCORDS.get(nomAccord).get("bouton"));
            accord.setTitle(ACCORDS.get(nomAccord).get("title"));

            accordRepository.save(accord);
        });
    }

}
