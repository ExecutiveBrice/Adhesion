#!/usr/bin/env bash
set -Eeuo pipefail

readonly compose_file="docker-compose.backend.yml"
readonly service="app_adhesion"
readonly container="app_adhesion"

: "${BACKEND_IMAGE_WITH_TAG:?BACKEND_IMAGE_WITH_TAG doit etre defini}"

if [[ ! -f .env ]]; then
  echo "Le fichier $(pwd)/.env est absent. Utilisez .env.backend.example comme modele." >&2
  exit 1
fi

export BACKEND_IMAGE_WITH_TAG
docker compose --env-file .env -f "$compose_file" config --quiet

previous_image="$(docker inspect --format '{{.Config.Image}}' "$container" 2>/dev/null || true)"

echo "Deploiement de $BACKEND_IMAGE_WITH_TAG"
docker compose --env-file .env -f "$compose_file" pull "$service"

if docker compose --env-file .env -f "$compose_file" up -d --no-deps --wait --wait-timeout 180 "$service"; then
  echo "Backend deploye et sain: $BACKEND_IMAGE_WITH_TAG"
  exit 0
fi

echo "Le nouveau conteneur n'est pas sain." >&2
docker compose --env-file .env -f "$compose_file" logs --tail=100 "$service" >&2 || true

if [[ -z "$previous_image" || "$previous_image" == "$BACKEND_IMAGE_WITH_TAG" ]]; then
  echo "Aucune image precedente distincte n'est disponible pour le rollback." >&2
  exit 1
fi

echo "Rollback vers $previous_image" >&2
BACKEND_IMAGE_WITH_TAG="$previous_image" \
  docker compose --env-file .env -f "$compose_file" up -d --no-deps --wait --wait-timeout 180 "$service"

echo "Rollback termine. Le deploiement reste signale en echec." >&2
exit 1
