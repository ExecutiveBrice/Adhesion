# Adhesion

Application de gestion associative : serveur Spring Boot dans `app`, interface Angular dans `client`.

## Développer et faire évoluer le projet

- [Référentiel fonctionnel et architecture](docs/README.md)
- [Instructions pour Codex et les contributions](AGENTS.md)
- [Commandes de validation et CI](docs/quality/validation.md)
- [Critères de fin de tâche](docs/quality/definition-of-done.md)

Prérequis : Java 25, Maven 3.9+, Node 24 et PostgreSQL pour le serveur.
Dans `client`, installer avec `npm ci`, puis démarrer avec `npm start`.
Le démarrage du serveur exige une configuration locale de base et de services ; ne pas utiliser
les identifiants de production pour les tests. Les tests backend isolent leurs dépendances.

L'API locale est documentée sur [Swagger](http://localhost:8000/swagger-ui/index.html).

## Contrôles rapides

```sh
node scripts/check-specs.mjs
node --test scripts/check-specs.test.mjs
```

Dans `app` : `mvn -B verify`. Dans `client` : `npm run test:ci`,
`npm run build:production`, puis `npm run test:browser` après installation de Chromium.
Le [guide de validation](docs/quality/validation.md) précise le périmètre et les limites de chaque contrôle.
