#!/usr/bin/env bash
# Daily encrypted PostgreSQL backup for Sauda (run on the deploy server via CI or cron).
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/sauda}"
BACKUP_DIR="${BACKUP_DIR:-${APP_DIR}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
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

mkdir -p "${BACKUP_DIR}"

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  echo "Docker Compose not found"
  exit 1
fi

if ! ${COMPOSE} ps --status running postgres | grep -q postgres; then
  echo "PostgreSQL container is not running"
  exit 1
fi

TIMESTAMP="$(date -u +%Y%m%d_%H%M%S)"
FILENAME="${DB_NAME}_${TIMESTAMP}.sql.gz.gpg"
BACKUP_PATH="${BACKUP_DIR}/${FILENAME}"

echo "Creating encrypted backup: ${BACKUP_PATH}"
${COMPOSE} exec -T postgres pg_dump \
  --no-owner \
  --no-acl \
  -U "${DB_USER}" \
  -d "${DB_NAME}" \
  | gzip -9 \
  | gpg --symmetric --cipher-algo AES256 --batch --yes --passphrase-file "${PASSPHRASE_FILE}" \
  > "${BACKUP_PATH}"

BYTES="$(wc -c < "${BACKUP_PATH}" | tr -d ' ')"
if [[ "${BYTES}" -lt 1024 ]]; then
  echo "Backup file is suspiciously small (${BYTES} bytes)"
  exit 1
fi

echo "Backup size: ${BYTES} bytes"
echo "Pruning encrypted backups older than ${RETENTION_DAYS} days"
find "${BACKUP_DIR}" -maxdepth 1 -name '*.sql.gz.gpg' -mtime +"${RETENTION_DAYS}" -delete
find "${BACKUP_DIR}" -maxdepth 1 -name '*.sql.gz' -mtime +"${RETENTION_DAYS}" -delete
ls -lh "${BACKUP_DIR}"/*.sql.gz.gpg 2>/dev/null | tail -5 || true
