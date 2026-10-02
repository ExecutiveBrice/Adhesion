# Bouchon local HelloAsso

Ce Docker Compose démarre WireMock sur `http://localhost:8089` et couvre les
trois appels HelloAsso utilisés par la boutique : OAuth, création d'une intention
de checkout et lecture de son état.

```powershell
cd extra
docker compose up -d
```

Pour que le backend utilise le bouchon, fournir les variables suivantes au
démarrage :

```powershell
$env:SPRING_PROFILES_ACTIVE = 'helloasso-sandbox'
$env:HELLOASSO_BASE_URL = 'http://localhost:8089'
$env:HELLOASSO_CLIENT_ID = 'local-client'
$env:HELLOASSO_CLIENT_SECRET = 'local-secret'
$env:HELLOASSO_ORGANIZATION_SLUG = 'local-organization'
```

Si le backend tourne dans son conteneur Docker plutôt que directement sur la
machine, utiliser `http://host.docker.internal:8089` pour `HELLOASSO_BASE_URL`.

Le bouchon retourne l'URL de retour envoyée à HelloAsso : le navigateur revient
donc immédiatement sur l'écran de vérification de la commande. Toute intention
est confirmée avec un paiement `Authorized` de 2 500 centimes. Le backend ne
compare pas ce montant au total de la commande pour HelloAsso, dont le paiement
peut inclure une contribution supplémentaire.

Les requêtes et mappings sont consultables dans l'interface d'administration :
<http://localhost:8089/__admin>.
