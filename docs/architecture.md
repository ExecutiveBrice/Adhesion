# Architecture et frontières

## Organisation actuelle

`app` contient le serveur Spring Boot, la sécurité, la persistence JPA et Liquibase.
`client` contient l'interface Angular. Les services `_services` portent les échanges HTTP et les
pages ou composants la présentation. Les types JSON d'échange doivent conserver leur compatibilité.

Le serveur historique est organisé par couches (`controllers`, `services`, `repository`, `models`).
La boutique dispose déjà de domaines `shop/catalog`, `shop/order` et `shop/payment`.
Cette organisation reste en place : déplacer un fichier sans clarifier une responsabilité ne réduit pas le couplage.

## Première frontière extraite

`domain/chat/ChatAccessPolicy` contient les décisions de lecture et d'écriture des messages.
Elle dépend des modèles existants et de Java, sans accès à la base, contrôleur ou service Spring.
`ChatService` charge les liens actuels, appelle cette politique, puis orchestre les messages et accusés.
Les tests de comportement du service restent la protection des règles CHAT-001 à CHAT-003.

Les modèles historiques portent des annotations JPA : cette extraction est une première séparation
des décisions et des effets, pas une conversion générale vers des modèles sans persistence.

## Dépendances à conserver

- HTTP → service d'application → décisions métier et repositories.
- Les décisions de `domain` n'importent pas contrôleurs, repositories, services d'application ou Spring.
- Un contrôleur HTTP déclare sa sécurité ou une route publique explicitement documentée.
- Les appels aux prestataires restent derrière leurs interfaces ; les tests n'exigent pas de compte réel.
- Les composants Angular utilisent les services HTTP et gardent leurs états de présentation localement.

`DomainArchitectureTest` et `ControllerAuthorizationArchitectureTest` vérifient les deux premières
frontières automatisées. Les frontières métier restantes sont examinées en revue.

## Évolutions progressives

| Zone | Extraction possible | Protection préalable |
| --- | --- | --- |
| AdhesionServices | Choix de statut, capacité et tarif | Valider les transitions ; couvrir MEMBER-003 |
| AdherentServices | Gestion d'identité, accords, exports | Tests d'e-mail, tribu et conservation des exports |
| board-admin.component | Éditeurs par domaine | Parcours navigateur par éditeur concerné |
| boutique | Conserver catalogue / commande / paiement | États, montants, idempotence et concurrence |

Ces pistes ne sont pas des changements métier autorisés par elles-mêmes. Extraire une responsabilité
à l'occasion d'un besoin concret, avec les comportements fixés par des tests avant la modification.
Pour une décision structurante, ajouter un ADR court dans `docs/decisions` : contexte, décision,
conséquences et vérification. [Premier ADR](decisions/0001-functional-contracts.md).
