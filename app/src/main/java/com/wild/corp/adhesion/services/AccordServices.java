package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Accord;
import com.wild.corp.adhesion.repository.AccordRepository;
import com.wild.corp.adhesion.repository.ParamTextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.wild.corp.adhesion.utils.Accords.*;
import static java.util.Map.entry;

@Service
public class AccordServices {



    public static final Map<String, Map<String, String>> ACCORDS = Map.ofEntries(
            entry(REGLEMENT_INTERIEUR, Map.ofEntries(entry("title", "Règlement intérieur"), entry("valide", "J'accèpte le règlement intérieur"), entry("refusable", "false"))),
            entry(ATTESTATION_SANTE, Map.ofEntries(entry("title", "Attestation santé"), entry("valide", "J'atteste de ma bonne santé"), entry("refus", "Je demande un certificat médical"), entry("refusable", "true"))),
            entry(VIE_CLUB, Map.ofEntries(entry("title", "Vie du club"), entry("valide", "En adhérent, j'accèpte de participer à la vie du club"), entry("refusable", "false"))),
            entry(AUTORISATION_PARENTALE, Map.ofEntries(entry("title", "Autorisation Parentale"), entry("valide", "Je donne mon autorisation parentale"), entry("refusable", "false"))),
            entry(PRISE_EN_CHARGE, Map.ofEntries(entry("title", "Prise en charge"), entry("valide", "J'accepte la prise en charge de mon enfant"), entry("refus", "Je refuse la prise en charge de mon enfant"),entry("refusable", "true"))),
            entry(RGPD, Map.ofEntries(entry("title", "Autorisation d'utilisation des données personnelles"), entry("valide", "J'accepte l'utilisation de mes données personnelles"), entry("refusable", "false"))),
            entry(DROIT_IMAGE, Map.ofEntries(entry("title", "Droit à l'image"), entry("valide", "J'accèpte l'utilisation de mon image"),entry("refus", "Je refuse l'utilisation de mon image"), entry("refusable", "true")))
    );

    @Autowired
    AccordRepository accordRepository;

    @Autowired
    ParamTextRepository paramTextRepository;

    public Accord createAccord(String accordKey) {
        String paramText = paramTextRepository.findByParamName(accordKey).get().getParamValue();
        Accord newAccord = new Accord(accordKey, ACCORDS.get(accordKey).get("title"), ACCORDS.get(accordKey).get("valide"), ACCORDS.get(accordKey).get("refus"), Boolean.parseBoolean(ACCORDS.get(accordKey).get("refusable")), paramText);
        newAccord.setEtat(true);
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
            if ("Autorisation d'utilisation des données personnelles".equals(nomAccord)) {
                nomAccord = "RGPD";
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
            accord.setValide(ACCORDS.get(nomAccord).get("valide"));
            accord.setRefus(ACCORDS.get(nomAccord).get("refus"));
            accord.setTitle(ACCORDS.get(nomAccord).get("title"));

            accordRepository.save(accord);
        });
    }

}
