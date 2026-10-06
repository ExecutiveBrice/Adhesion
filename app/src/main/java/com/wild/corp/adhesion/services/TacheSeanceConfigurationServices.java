package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.TacheSeance;
import com.wild.corp.adhesion.models.resources.TacheSeanceConfiguration;
import com.wild.corp.adhesion.repository.TacheSeanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class TacheSeanceConfigurationServices {
    private static final Set<String> ICONES_AUTORISEES = Set.of(
            "clipboard-check", "clipboard-list", "box-open", "screwdriver-wrench", "broom",
            "people-group", "handshake", "car", "van-shuttle", "basketball", "futbol", "volleyball",
            "dumbbell", "music", "utensils", "mug-hot", "key", "door-open", "kit-medical", "bullhorn",
            "camera", "flag", "stopwatch", "trophy", "table", "chair", "shirt", "euro-sign");
    private final TacheSeanceRepository repository;

    public TacheSeanceConfigurationServices(TacheSeanceRepository repository) {
        this.repository = repository;
    }

    public List<TacheSeanceConfiguration> getAll() {
        return repository.findAllByOrderByNomAsc().stream().map(this::response).toList();
    }

    public TacheSeanceConfiguration create(TacheSeanceConfiguration configuration) {
        String nom = normaliser(configuration);
        if (repository.existsByNomIgnoreCase(nom)) throw doublon();
        TacheSeance tache = new TacheSeance();
        tache.setNom(nom);
        appliquerPresentation(tache, configuration);
        return response(repository.save(tache));
    }

    public TacheSeanceConfiguration update(Long id, TacheSeanceConfiguration configuration) {
        TacheSeance tache = trouver(id);
        String nom = normaliser(configuration);
        if (repository.existsByNomIgnoreCaseAndIdNot(nom, id)) throw doublon();
        tache.setNom(nom);
        appliquerPresentation(tache, configuration);
        return response(repository.save(tache));
    }

    public void delete(Long id) {
        TacheSeance tache = trouver(id);
        if (repository.isUsed(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette tâche est associée à une séance. Retirez ses responsabilités avant de la supprimer.");
        }
        repository.delete(tache);
    }

    private TacheSeance trouver(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche introuvable"));
    }

    private String normaliser(TacheSeanceConfiguration configuration) {
        String nom = configuration == null || configuration.nom() == null ? "" : configuration.nom().trim();
        if (nom.isBlank() || nom.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom de la tâche est obligatoire et limité à 100 caractères");
        }
        return nom;
    }

    private ResponseStatusException doublon() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une tâche portant ce nom existe déjà");
    }

    private void appliquerPresentation(TacheSeance tache, TacheSeanceConfiguration configuration) {
        // Preserve existing presentation when an older client only sends the task name.
        if (configuration.icone() != null) {
            String icone = configuration.icone().trim();
            if (!ICONES_AUTORISEES.contains(icone)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez une icône parmi les icônes proposées");
            }
            tache.setIcone(icone);
        }
        if (configuration.couleur() != null) {
            String couleur = configuration.couleur().trim().toUpperCase(Locale.ROOT);
            if (!couleur.matches("#[0-9A-F]{6}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La couleur de la tâche doit être au format #RRGGBB");
            }
            tache.setCouleur(couleur);
        }
    }

    private TacheSeanceConfiguration response(TacheSeance tache) {
        return new TacheSeanceConfiguration(tache.getId(), tache.getNom(), tache.getIcone(), tache.getCouleur());
    }
}
