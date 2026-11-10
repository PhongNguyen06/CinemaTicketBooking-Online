#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
  echo "Usage: ./restore.sh /path/to/cinema_booking.sql.gz" >&2; exit 1
fi
printf 'RESTORE WILL OVERWRITE DATA. Type RESTORE to continue: '
read -r answer
[ "$answer" = "RESTORE" ] || exit 2
# Stop API during restoration, avoiding concurrent writes.
docker compose --env-file .env -f compose.prod.yaml stop api
gzip -dc "$1" | docker compose --env-file .env -f compose.prod.yaml exec -T mysql   sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root'
docker compose --env-file .env -f compose.prod.yaml start api
