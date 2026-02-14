# Proposition d’intégration HelloAsso (API v5) dans Adhesion

## Objectifs
- Permettre un paiement en ligne HelloAsso depuis le parcours d’adhésion.
- Réconcilier automatiquement les paiements HelloAsso avec les `Adhesion` et `Paiement` existants.
- Conserver un fallback manuel pour le secrétariat (virements/chèques déjà gérés).

## Constat sur le projet actuel
- Le backend dispose déjà d’un modèle `Paiement` rattaché à `Adhesion` (`List<Paiement>`), ce qui donne un bon point d’entrée pour enregistrer les règlements en provenance d’HelloAsso.
- L’API interne expose déjà des endpoints de création/suppression de paiements (`/adhesion/savePaiement`, `/adhesion/deletePaiement`) et de validation du paiement côté secrétariat (`/adhesion/updatePaiementSecretariat`).
- Une spécification OpenAPI HelloAsso est déjà présente dans le repo (`app/src/main/resources/helloasso.json`), donc le besoin est surtout l’intégration métier/sécurité.

## Architecture cible (simple et robuste)

### 1) Backend Spring : module d’intégration dédié
Créer une couche dédiée, par exemple :
- `services/helloasso/HelloAssoAuthService` : récupère et met en cache le token OAuth2 client_credentials.
- `services/helloasso/HelloAssoCheckoutService` : crée un checkout intent (`POST /v5/organizations/{slug}/checkout-intents`).
- `services/helloasso/HelloAssoWebhookService` : traite les événements de paiement asynchrones.
- `controllers/HelloAssoController` : endpoints internes exposés au front.

### 2) Flux “paiement en ligne”
1. Le front demande au backend de créer une session de paiement pour une `adhesionId`.
2. Le backend appelle HelloAsso pour créer le checkout intent et retourne `redirectUrl` au front.
3. Le front redirige l’utilisateur vers HelloAsso.
4. À la fin du paiement, HelloAsso notifie votre webhook backend.
5. Le backend mappe l’événement sur `Adhesion`, crée un `Paiement`, et positionne `validPaiementSecretariat=true` si vous souhaitez une validation automatique des paiements CB.

### 3) Corrélation des données
Le point critique est de lier de façon fiable un paiement HelloAsso à une adhésion interne.

Recommandation :
- Générer une référence interne unique (ex: `ADH-{adhesionId}-{timestamp}`) lors de la création du checkout.
- La transmettre dans les métadonnées/description du checkout intent.
- À réception webhook, rechercher l’adhésion via cette référence.

## Endpoints backend proposés

### `POST /helloasso/checkout`
Entrée:
```json
{
  "adhesionId": 123,
  "returnUrl": "https://votre-front/paiement/success",
  "cancelUrl": "https://votre-front/paiement/cancel"
}
```

Sortie:
```json
{
  "checkoutIntentId": 456789,
  "redirectUrl": "https://www.helloasso.com/associations/...",
  "reference": "ADH-123-1710000000"
}
```

### `POST /helloasso/webhook`
- Endpoint appelé par HelloAsso.
- Vérifie l’authenticité (signature/token webhook si configuré dans HelloAsso).
- Idempotence obligatoire (ne pas créer 2 paiements pour le même `orderId`).

### `GET /helloasso/checkout/{checkoutIntentId}`
- Endpoint de support pour vérifier l’état d’un checkout intent côté BO (optionnel mais utile au secrétariat).

## Mapping métier recommandé
Lors d’un paiement confirmé :
- Créer un `Paiement` avec:
  - `dateReglement = date du paiement`
  - `typeReglement = "HELLOASSO"`
  - `montant = montant payé (centimes -> euros selon convention interne)`
- Ajouter ce `Paiement` à l’`Adhesion` correspondante.
- Mettre à jour `validPaiementSecretariat` selon votre règle métier.

## Sécurité & conformité
- Secrets en variables d’environnement (jamais en dur) :
  - `HELLOASSO_CLIENT_ID`
  - `HELLOASSO_CLIENT_SECRET`
  - `HELLOASSO_ORGANIZATION_SLUG`
  - `HELLOASSO_WEBHOOK_SECRET` (si utilisé)
- Journaliser les échanges sans exposer tokens/PII.
- Mettre en place des timeouts/retries et circuit breaker léger pour l’API externe.

## Modifications techniques minimales à prévoir

### `application.yml`
Ajouter une section typée :
```yaml
helloasso:
  base-url: https://api.helloasso.com/v5
  organization-slug: ${HELLOASSO_ORGANIZATION_SLUG}
  client-id: ${HELLOASSO_CLIENT_ID}
  client-secret: ${HELLOASSO_CLIENT_SECRET}
  webhook-secret: ${HELLOASSO_WEBHOOK_SECRET:}
```

### Backend
- Ajouter des DTOs d’échange (`CheckoutRequest`, `CheckoutResponse`, `WebhookEvent`).
- Ajouter un client HTTP (WebClient ou RestTemplate) pour HelloAsso.
- Ajouter une table d’idempotence webhook (ex: `helloasso_events`) ou une contrainte unique sur identifiant d’ordre.

### Frontend Angular
- Ajouter un bouton “Payer en ligne (HelloAsso)” sur l’écran d’adhésion.
- Appeler `POST /helloasso/checkout` puis redirection navigateur vers `redirectUrl`.
- Ajouter une page de retour (`/paiement/success` / `/paiement/cancel`) qui relit l’état adhésion.

## Stratégie de mise en œuvre en 3 lots
1. **Lot 1 (MVP)**: création checkout + redirection + webhook + création `Paiement`.
2. **Lot 2**: robustesse (idempotence stricte, monitoring, retries, logs structurés).
3. **Lot 3**: UX BO (écran de suivi des transactions HelloAsso, rapprochement manuel assisté).

## Risques principaux à anticiper
- Paiement validé chez HelloAsso mais webhook non reçu (prévoir job de réconciliation périodique via API HelloAsso).
- Doublons webhook (idempotence obligatoire).
- Différences de format montant/taxes/commission selon le type de formulaire HelloAsso.

## Proposition concrète pour ton repo
Je te recommande de démarrer par une intégration backend-only très cadrée :
- Nouveau package `com.wild.corp.adhesion.integration.helloasso`.
- 1 controller (`HelloAssoController`) + 2 services (`Auth`, `Checkout`) + 1 service webhook.
- Réutilisation directe de ton modèle `Paiement` existant (pas de refonte DB initiale, uniquement une table d’idempotence).

Ensuite, côté front, un simple CTA “Payer en ligne” dans la vue adhésion suffit pour déclencher le flux.
