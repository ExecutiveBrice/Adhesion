package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Salle;
import com.wild.corp.adhesion.models.resources.SalleConfiguration;
import com.wild.corp.adhesion.repository.SalleRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class SalleConfigurationServices {

    private final SalleRepository salleRepository;

    public SalleConfigurationServices(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Transactional
    public List<SalleConfiguration> getAll() {
        return salleRepository.findAllByOrderByNomAsc().stream()
                .map(this::toConfiguration)
                .toList();
    }

    @Transactional
    public SalleConfiguration create(SalleConfiguration configuration) {
        SalleConfiguration normalisee = normaliser(configuration);
        Salle salle = Salle.builder()
                .nom(normalisee.nom())
                .adresse(normalisee.adresse())
                .couleur(normalisee.couleur())
                .build();
        return toConfiguration(salleRepository.save(salle));
    }

    @Transactional
    public SalleConfiguration update(Long id, SalleConfiguration configuration) {
        Salle salle = salleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
        SalleConfiguration normalisee = normaliser(configuration);
        salle.setNom(normalisee.nom());
        salle.setAdresse(normalisee.adresse());
        salle.setCouleur(normalisee.couleur());
        return toConfiguration(salleRepository.save(salle));
    }

    @Transactional
    public void delete(Long id) {
        Salle salle = salleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salle introuvable"));
        salleRepository.delete(salle);
    }

    private SalleConfiguration normaliser(SalleConfiguration configuration) {
        if (configuration == null) {
            throw configurationInvalide("Les informations de la salle sont obligatoires");
        }
        String nom = nettoyer(configuration.nom());
        String adresse = nettoyer(configuration.adresse());
        String couleur = configuration.couleur();
        if (nom.isBlank() || nom.length() > 100) {
            throw configurationInvalide("Le nom de la salle est obligatoire et limité à 100 caractères");
        }
        if (adresse.isBlank() || adresse.length() > 500) {
            throw configurationInvalide("L'adresse de la salle est obligatoire et limitée à 500 caractères");
        }
        if (couleur == null || !couleur.matches("#[0-9a-fA-F]{6}")) {
            throw configurationInvalide("La couleur de la salle doit être au format hexadécimal");
        }
        return new SalleConfiguration(null, nom, adresse, couleur.toUpperCase(Locale.ROOT));
    }

    private String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private SalleConfiguration toConfiguration(Salle salle) {
        return new SalleConfiguration(salle.getId(), salle.getNom(), salle.getAdresse(), salle.getCouleur());
    }

    private ResponseStatusException configurationInvalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
