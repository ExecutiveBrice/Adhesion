# Adhesion

Documentation OpenAPI locale : <http://localhost:8000/swagger-ui/index.html>

## CI/CD du backend vers le VPS OVH

Deux workflows GitHub Actions sont disponibles :

- `Backend CI` compile, teste et construit l'image sur chaque pull request qui modifie `app/` ;
- `Backend - Deploy production` teste, publie une image immuable dans GHCR puis deploie le backend sur le VPS apres chaque push sur `master`. Il peut aussi etre lance manuellement.

Le deploiement ne modifie ni le frontend, ni PostgreSQL. Il remplace uniquement le conteneur `app_adhesion`, attend que `/actuator/health` soit sain et revient automatiquement a l'image precedente en cas d'echec.

### 1. Preparer le VPS

Le VPS doit disposer de Docker et de Docker Compose v2. Le conteneur PostgreSQL existant (`db_adhesion`) et Traefik doivent partager le reseau Docker utilise par le backend. Par defaut, ce reseau est `adhesion_traefik_web`.

Depuis le poste local, copier le modele d'environnement vers le compte de deploiement :

```bash
ssh <utilisateur>@<hote> 'mkdir -p ~/adhesion'
scp deploy/.env.backend.example <utilisateur>@<hote>:adhesion/.env
ssh <utilisateur>@<hote> 'chmod 600 ~/adhesion/.env'
```

Renseigner ensuite toutes les valeurs de `~/adhesion/.env`. Pour conserver les images deja stockees par l'application, `APP_DATA_DIR` doit pointer vers leur repertoire actuel. Le fichier `.env` reste uniquement sur le VPS et n'est jamais remplace par le pipeline.

Le compte SSH doit pouvoir executer `docker` sans interaction et ecrire dans `~/adhesion`.

### 2. Configurer GitHub

Creer l'environnement GitHub `production`, puis y definir les secrets suivants :

| Secret | Contenu |
| --- | --- |
| `OVH_HOST` | Nom DNS ou adresse IP du VPS |
| `OVH_USER` | Utilisateur SSH de deploiement |
| `OVH_SSH_PRIVATE_KEY` | Cle privee SSH dediee au deploiement |
| `OVH_SSH_KNOWN_HOSTS` | Ligne `known_hosts` du VPS, obtenue et verifiee par exemple avec `ssh-keyscan -H <hote>` (ajouter `-p <port>` si necessaire) |
| `GHCR_USERNAME` | Utilisateur GitHub autorise a lire le package |
| `GHCR_TOKEN` | Personal Access Token classique avec la permission `read:packages` |

Si SSH n'ecoute pas sur le port 22, ajouter la variable d'environnement GitHub `OVH_SSH_PORT`.

Il est recommande d'utiliser une cle SSH dediee et sans droits administrateur. La cle publique correspondante doit etre ajoutee dans `~/.ssh/authorized_keys` sur le VPS.

### 3. Premier deploiement

Verifier que `~/adhesion/.env`, le conteneur PostgreSQL et le reseau Docker sont disponibles, puis lancer manuellement `Backend - Deploy production` sur la branche `master` depuis l'onglet Actions. Les deploiements suivants partiront automatiquement des changements backend fusionnes sur `master`.
