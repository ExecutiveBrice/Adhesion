package com.wild.corp.adhesion.security;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;

/**
 * Inventaire volontairement restreint des routes accessibles sans session.
 * Toute nouvelle route publique doit être ajoutée ici et justifiée dans son
 * commentaire, puis couverte par le test d'architecture.
 */
public final class PublicApiEndpoints {

    public record Endpoint(HttpMethod method, String path, String purpose) {
    }

    public static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(HttpMethod.POST, "/auth/signin", "Connexion"),
            new Endpoint(HttpMethod.POST, "/auth/reinitPassword", "Demande de réinitialisation de mot de passe"),
            new Endpoint(HttpMethod.POST, "/auth/changePassword", "Confirmation de réinitialisation avec un jeton à usage unique"),
            new Endpoint(HttpMethod.GET, "/auth/confirmEmail/{confirmationToken}", "Confirmation de l'adresse e-mail"),
            new Endpoint(HttpMethod.GET, "/param/agendas", "Agendas publics affichés sur l'accueil"),
            new Endpoint(HttpMethod.GET, "/param/salles", "Lieux publics affichés sur le calendrier"),
            new Endpoint(HttpMethod.GET, "/param/allText", "Contenus publics de l'accueil"),
            new Endpoint(HttpMethod.GET, "/param/allBoolean", "Indicateur public d'ouverture des inscriptions"),
            new Endpoint(HttpMethod.GET, "/param/isClose", "Indicateur public de maintenance"),
            new Endpoint(HttpMethod.GET, "/activite/calendrier", "Calendrier public des activités"),
            new Endpoint(HttpMethod.GET, "/activite/calendrier/google", "Événements des agendas publics")
    );

    private static final Set<EndpointKey> KEYS = ENDPOINTS.stream()
            .map(endpoint -> new EndpointKey(endpoint.method(), endpoint.path()))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private PublicApiEndpoints() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        return KEYS.contains(new EndpointKey(method, path));
    }

    private record EndpointKey(HttpMethod method, String path) {
    }
}
