#!/usr/bin/env bash
set -euo pipefail

COMPOSE="docker compose -f docker-compose.prod.yml --env-file .env.prod"

cd "$(dirname "$0")/.."

git pull

echo "==> tearing down (volumes wiped)"
$COMPOSE down -v

echo "==> building and starting"
$COMPOSE up -d --build

echo "==> done"
