#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p backups
chmod 700 backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="backups/cinema_booking_${stamp}.sql.gz"
# Credentials are read inside the DB container, not passed as shell command arguments.
docker compose --env-file .env -f compose.prod.yaml exec -T mysql   sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -u root --single-transaction --routines --triggers --events --databases cinema_booking'   | gzip > "$file"
chmod 600 "$file"
echo "Backup stored at $file. Copy it to secure off-server storage."
