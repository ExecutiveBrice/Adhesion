# ADR 0001 — Règles fonctionnelles reliées aux tests

Date : 2026-10-09. Statut : adopté pour le processus de contribution.

## Contexte

Les fonctionnalités évoluent avec Codex. Le dépôt possède des tests et plusieurs documents métier,
mais leur relation et les contrôles de livraison ne sont pas centralisés.

## Décision

Conserver des fiches Markdown courtes avec IDs stables et une matrice JSON de couverture.
AGENTS.md fournit la méthode de contribution. GitHub Actions exécute les contrôles indépendamment
du compte rendu de Codex. Le modèle de PR et la définition de terminé rendent les preuves visibles.

Extraire progressivement les décisions métier, en commençant par les accès au chat. Éviter une
réorganisation générale des packages et toute nouvelle dépendance backend pour cette première étape.
Les parcours navigateur utilisent Playwright avec une API simulée et échouent sur les requêtes non prévues.

## Conséquences

Une règle issue du code est marquée Observée. Les lacunes de couverture restent visibles.
La validation des liens ne remplace pas les assertions ni une revue métier.
La chaîne réelle navigateur + serveur et les paiements HelloAsso nécessitent des validations complémentaires.
