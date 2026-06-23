#!/usr/bin/env bash
# Restore PostgreSQL from an encrypted Sauda backup (.sql.gz.gpg).
#
# Usage:
#   BACKUP_GPG_PASSPHRASE='...' ./restore-postgres.sh backups/sauda_prod_20250623_020000.sql.gz.gpg
#   BACKUP_GPG_PASSPHRASE_FILE=/path/to/passphrase ./restore-postgres.sh <backup-file>
#
# Optional:
#   RECREATE_DB=true  — drop and recreate target database before restore (recommended)
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/sauda}"
BACKUP_FILE="${1:?Usage: restore-postgres.sh <backup.sql.gz.gpg>}"
PASSPHRASE_FILE=""

cleanup() {
  if [[ -n "${PASSPHRASE_FILE}" && "${PASSPHRASE_FILE}" != "${BACKUP_GPG_PASSPHRASE_FILE:-}" ]]; then
    shred -u "${PASSPHRASE_FILE}" 2>/dev/null || rm -f "${PASSPHRASE_FILE}"
  fi
}
trap cleanup EXIT

cd "${APP_DIR}"

if [[ ! -f .env ]]; then
  echo "Missing ${APP_DIR}/.env"
  exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Backup file not found: ${BACKUP_FILE}"
  exit 1
fi

# shellcheck disable=SC1091
set -a
source .env
set +a

: "${DB_NAME:?DB_NAME is not set in .env}"
: "${DB_USER:?DB_USER is not set in .env}"

if [[ -z "${BACKUP_GPG_PASSPHRASE_FILE:-}" && -z "${BACKUP_GPG_PASSPHRASE:-}" ]]; then
  echo "Set BACKUP_GPG_PASSPHRASE or BACKUP_GPG_PASSPHRASE_FILE"
  exit 1
fi

if ! command -v gpg >/dev/null 2>&1; then
  echo "gpg not found. Install: sudo apt install -y gnupg"
  exit 1
fi

if [[ -n "${BACKUP_GPG_PASSPHRASE_FILE:-}" ]]; then
  PASSPHRASE_FILE="${BACKUP_GPG_PASSPHRASE_FILE}"
else
  PASSPHRASE_FILE="$(mktemp)"
  chmod 600 "${PASSPHRASE_FILE}"
  printf '%s' "${BACKUP_GPG_PASSPHRASE}" > "${PASSPHRASE_FILE}"
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  echo "Docker Compose not found"
  exit 1
fi

echo "Stopping backend to release DB connections..."
${COMPOSE} stop backend || true

if [[ "${RECREATE_DB:-false}" == "true" ]]; then
  echo "Recreating database ${DB_NAME}..."
  ${COMPOSE} exec -T postgres psql -U "${DB_USER}" -d postgres -v ON_ERROR_STOP=1 <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};
SQL
fi

echo "Restoring from ${BACKUP_FILE} into ${DB_NAME}..."
gpg --decrypt --batch --yes --passphrase-file "${PASSPHRASE_FILE}" "${BACKUP_FILE}" \
  | gunzip -c \
  | ${COMPOSE} exec -T postgres psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}"

echo "Starting backend..."
${COMPOSE} start backend

echo "Restore completed."
