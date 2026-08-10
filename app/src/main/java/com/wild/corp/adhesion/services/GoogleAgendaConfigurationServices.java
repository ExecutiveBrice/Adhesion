package com.wild.corp.adhesion.services;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.wild.corp.adhesion.models.GoogleAgenda;
import com.wild.corp.adhesion.models.ParamText;
import com.wild.corp.adhesion.models.resources.AgendaGoogleConfiguration;
import com.wild.corp.adhesion.repository.GoogleAgendaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GoogleAgendaConfigurationServices {

    private static final String PARAMETRE_AGENDAS = "Google_Agendas";
    private static final int MAX_AGENDAS = 10;
    private static final String[] COULEURS_PAR_DEFAUT = {
            "#4285F4", "#DB4437", "#F4B400", "#0F9D58", "#AB47BC",
            "#00ACC1", "#FF7043", "#5C6BC0", "#9E9D24", "#8D6E63"
    };

    private final GoogleAgendaRepository googleAgendaRepository;
    private final ParamTextServices paramTextServices;
    private final GoogleAgendaServices googleAgendaServices;
    private final JsonMapper jsonMapper;

    public GoogleAgendaConfigurationServices(GoogleAgendaRepository googleAgendaRepository,
                                              ParamTextServices paramTextServices,
                                              GoogleAgendaServices googleAgendaServices,
                                              JsonMapper jsonMapper) {
        this.googleAgendaRepository = googleAgendaRepository;
        this.paramTextServices = paramTextServices;
        this.googleAgendaServices = googleAgendaServices;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public List<AgendaGoogleConfiguration> getAll() {
        migrerAncienneConfiguration();
        return googleAgendaRepository.findAllByOrderByNomAsc().stream()
                .map(this::toConfiguration)
                .toList();
    }

    @Transactional
    public AgendaGoogleConfiguration create(AgendaGoogleConfiguration configuration) {
        if (googleAgendaRepository.count() >= MAX_AGENDAS) {
            throw configurationInvalide("Le nombre d'agendas Google est limité à " + MAX_AGENDAS);
        }
        AgendaGoogleConfiguration normalisee = normaliser(configuration);
        if (googleAgendaRepository.existsBySource(normalisee.source())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet agenda Google est déjà configuré");
        }
        GoogleAgenda agenda = GoogleAgenda.builder()
                .nom(normalisee.nom())
                .source(normalisee.source())
                .couleur(normalisee.couleur())
                .build();
        return toConfiguration(googleAgendaRepository.save(agenda));
    }

    @Transactional
    public AgendaGoogleConfiguration update(Long id, AgendaGoogleConfiguration configuration) {
        GoogleAgenda agenda = googleAgendaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda Google introuvable"));
        AgendaGoogleConfiguration normalisee = normaliser(configuration);
        if (googleAgendaRepository.existsBySourceAndIdNot(normalisee.source(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet agenda Google est déjà configuré");
        }
        agenda.setNom(normalisee.nom());
        agenda.setSource(normalisee.source());
        agenda.setCouleur(normalisee.couleur());
        return toConfiguration(googleAgendaRepository.save(agenda));
    }

    @Transactional
    public void delete(Long id) {
        GoogleAgenda agenda = googleAgendaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda Google introuvable"));
        googleAgendaRepository.delete(agenda);
    }

    private AgendaGoogleConfiguration normaliser(AgendaGoogleConfiguration configuration) {
        if (configuration == null || configuration.source() == null || configuration.source().isBlank()) {
            throw configurationInvalide("L'identifiant public de l'agenda est obligatoire");
        }
        String nom = configuration.nom() == null ? "" : configuration.nom().trim();
        if (nom.isBlank() || nom.length() > 100) {
            throw configurationInvalide("Le nom de l'agenda est obligatoire et limité à 100 caractères");
        }
        String source = googleAgendaServices.extraireIdentifiant(configuration.source());
        String couleur = configuration.couleur();
        if (couleur == null || !couleur.matches("#[0-9a-fA-F]{6}")) {
            throw configurationInvalide("La couleur de l'agenda doit être au format hexadécimal");
        }
        return new AgendaGoogleConfiguration(null, nom, source, couleur.toUpperCase(Locale.ROOT));
    }

    private void migrerAncienneConfiguration() {
        if (googleAgendaRepository.count() > 0) {
            return;
        }
        String valeur = paramTextServices.getParamValueOrDefault(PARAMETRE_AGENDAS, "").trim();
        if (valeur.isBlank()) {
            return;
        }

        List<AgendaGoogleConfiguration> anciennesConfigurations = lireAncienneConfiguration(valeur);
        Map<String, GoogleAgenda> agendasUniques = new LinkedHashMap<>();
        for (int index = 0; index < anciennesConfigurations.size() && index < MAX_AGENDAS; index++) {
            AgendaGoogleConfiguration ancienne = anciennesConfigurations.get(index);
            try {
                String source = googleAgendaServices.extraireIdentifiant(ancienne.source());
                String nom = ancienne.nom() == null || ancienne.nom().isBlank()
                        ? nomParDefaut(source, index) : ancienne.nom().trim();
                String couleur = ancienne.couleur() != null && ancienne.couleur().matches("#[0-9a-fA-F]{6}")
                        ? ancienne.couleur().toUpperCase(Locale.ROOT)
                        : COULEURS_PAR_DEFAUT[index % COULEURS_PAR_DEFAUT.length];
                agendasUniques.putIfAbsent(source, GoogleAgenda.builder()
                        .nom(nom.substring(0, Math.min(nom.length(), 100)))
                        .source(source)
                        .couleur(couleur)
                        .build());
            } catch (ResponseStatusException ignored) {
                // Une ancienne ligne invalide ne doit pas bloquer les autres agendas.
            }
        }
        if (!agendasUniques.isEmpty()) {
            googleAgendaRepository.saveAll(agendasUniques.values());
        }
        paramTextServices.save(ParamText.builder()
                .paramName(PARAMETRE_AGENDAS)
                .paramValue("[]")
                .build());
    }

    private List<AgendaGoogleConfiguration> lireAncienneConfiguration(String valeur) {
        List<AgendaGoogleConfiguration> configurations = new ArrayList<>();
        if (valeur.startsWith("[")) {
            try {
                JsonNode racine = jsonMapper.readTree(valeur);
                if (racine.isArray()) {
                    for (JsonNode noeud : racine) {
                        configurations.add(new AgendaGoogleConfiguration(
                                null,
                                noeud.path("nom").asText(""),
                                noeud.path("source").asText(""),
                                noeud.path("couleur").asText("")));
                    }
                }
            } catch (Exception ignored) {
                return List.of();
            }
        } else {
            String[] sources = valeur.split("\\r?\\n|;");
            for (int index = 0; index < sources.length; index++) {
                if (!sources[index].isBlank()) {
                    configurations.add(new AgendaGoogleConfiguration(
                            null, "", sources[index].trim(),
                            COULEURS_PAR_DEFAUT[index % COULEURS_PAR_DEFAUT.length]));
                }
            }
        }
        return configurations;
    }

    private String nomParDefaut(String source, int index) {
        int arobase = source.indexOf('@');
        return arobase > 0 ? source.substring(0, arobase) : "Agenda Google " + (index + 1);
    }

    private AgendaGoogleConfiguration toConfiguration(GoogleAgenda agenda) {
        return new AgendaGoogleConfiguration(
                agenda.getId(), agenda.getNom(), agenda.getSource(), agenda.getCouleur());
    }

    private ResponseStatusException configurationInvalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
