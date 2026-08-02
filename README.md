Correction U11F enregistrement impossible

## Intégration HelloAsso (backend)

Le backend utilise maintenant des clients **Feign** pour appeler HelloAsso:
- `HelloAssoAuthClient` (OAuth2 token)
- `HelloAssoCheckoutClient` (checkout intents)

Variables d'environnement :
- `HELLOASSO_ORGANIZATION_SLUG`
- `HELLOASSO_CLIENT_ID`
- `HELLOASSO_CLIENT_SECRET`

Endpoints API:
- `POST /helloasso/checkout-intents`
- `GET /helloasso/checkout-intents/{checkoutIntentId}`
