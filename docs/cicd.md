# CI/CD

Пайплайны Sauda реализованы как GitHub Actions workflows в `.github/workflows/`.

## MVP: деплой отключён по умолчанию

Серверов пока нет — это нормально. **CI работает всегда** (сборка, тесты, quality gates).

CD (SSH-деплой) включается одной переменной, когда сервер будет готов:

**Settings → Secrets and variables → Actions → Variables → `DEPLOY_ENABLED` = `true`**

Пока переменная не задана или не равна `true`, deploy-workflows выводят notice и **не подключаются к серверу**.

Подробнее: [setup.md](setup.md), [infrastructure/deploy/README.md](../infrastructure/deploy/README.md)

## Обзор

```
┌─────────────┐     ┌─────────────┐
│  Backend CI │     │ Frontend CI │
│  (PR/push)  │     │  (PR/push)  │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └─────────┬─────────┘
                 │ merge
                 ▼
         ┌───────────────┐
         │ CD (optional) │
         │ DEPLOY_ENABLED│
         └───────┬───────┘
                 │
    ┌────────────┼────────────┐
    ▼            ▼            ▼
  DEV          TEST          PROD
 develop    release/*    manual only
```

## Continuous Integration

### Backend CI (`backend-ci.yml`)

**Триггеры:** pull request, push в `develop` или `main` (пути backend)

**Этапы:** Verify (Checkstyle, Spotless, тесты, JaCoCo ≥70%) → JAR → Docker build (без push)

### Frontend CI (`frontend-ci.yml`)

**Триггеры:** pull request, push в `develop` или `main` (пути frontend)

**Этапы:** Lint → Type check → Build → Tests → Docker build (без push)

### Политика ошибок

Любой упавший шаг — падение workflow.

### Hotfix и back-merge

| Workflow | Триггер | Действие |
|----------|---------|----------|
| `backend-ci.yml` / `frontend-ci.yml` | Push `hotfix/**`, PR → `main` | CI на hotfix |
| `back-merge-hotfix.yml` | Merge PR `hotfix/*` → `main` | Авто-PR `main` → `develop` |

После merge hotfix в `main` проверьте открытый PR **main → develop**, дождитесь CI и смержите.

**Настройка GitHub (обязательно для auto back-merge):**

Settings → Actions → General → **Allow GitHub Actions to create and approve pull requests** ✅

Альтернатива: repository secret **`REPO_PAT`** — Personal Access Token с правами `repo`.

Подробнее: [branching.md](branching.md)

## Continuous Deployment

Все deploy-workflows проверяют `vars.DEPLOY_ENABLED == 'true'`.

### Deploy DEV (`deploy-dev.yml`)

- **Триггер:** push в `develop` (или `workflow_dispatch`)
- **Environment:** `dev`
- **Профиль:** `dev`

### Deploy TEST (`deploy-test.yml`)

- **Триггер:** push в `release/**` (или `workflow_dispatch`)
- **Environment:** `test`
- **Профиль:** `test`

### Deploy PROD (`deploy-prod.yml`)

- **Триггер:** **только** `workflow_dispatch` (ручной запуск)
- **Push в `main` не деплоит** — только CI
- **Environment:** `production` (required reviewers)
- **Профиль:** `prod`
- **Параметр:** `git_ref` (по умолчанию `main`)

### Шаги деплоя (когда включён)

1. Сборка Docker-образов
2. Push в GHCR
3. SSH на сервер → `docker compose pull` → `docker compose up -d`

## Container Registry

```
ghcr.io/<org>/<repo>/sauda-api:<tag>
ghcr.io/<org>/<repo>/sauda-web:<tag>
```

Теги: `dev-latest`, `test-latest`, `prod-latest`, `<git-sha>`

## PostgreSQL Backup (`postgres-backup.yml`)

- **Триггер:** ежедневно в **02:00 UTC** (`dev` + `test`); PROD — только **workflow_dispatch** (с approval, если настроен)
- **Условие:** `DEPLOY_ENABLED=true` (как у CD)
- **Окружения:** `dev`, `test`, `production` — отдельный job на каждое (secrets с уровня Environment)
- **На сервере:** `/opt/sauda/backups/<db>_<timestamp>.sql.gz.gpg` (GPG AES-256)
- **Ротация:** переменная `BACKUP_RETENTION_DAYS` (по умолчанию 14)
- **На сервере нужен:** `sudo apt install -y gnupg`

### Настройка шифрования

1. Придумайте длинный пароль (passphrase) — **сохраните его в менеджере паролей**
2. GitHub → **Settings → Environments** → для каждого окружения (`dev`, `test`, `production`):
   - Secret: `BACKUP_GPG_PASSPHRASE` = ваш passphrase
3. Для разных серверов можно задать **разные** passphrase на уровне Environment

Файл без пароля **не открыть**. Расшифровка: `gpg --decrypt`.

### Восстановление PostgreSQL (подробно)

Скрипт: `infrastructure/deploy/restore-postgres.sh` (копируется на сервер вместе с репозиторием или вручную).

#### 1. Посмотреть доступные бэкапы

```bash
ssh user@your-server
cd /opt/sauda
ls -lh backups/
# пример: sauda_prod_20250623_020000.sql.gz.gpg
```

#### 2. Полное восстановление (рекомендуется)

Останавливает backend, пересоздаёт БД, восстанавливает данные, запускает backend.

```bash
cd /opt/sauda

# Подставьте реальный файл и passphrase из GitHub Secret BACKUP_GPG_PASSPHRASE
export BACKUP_GPG_PASSPHRASE='ваш-passphrase-из-github-secret'
export RECREATE_DB=true

chmod +x infrastructure/deploy/restore-postgres.sh
./infrastructure/deploy/restore-postgres.sh backups/sauda_prod_20250623_020000.sql.gz.gpg
```

#### 3. Восстановление вручную (пошагово)

```bash
cd /opt/sauda
set -a && source .env && set +a

# 1) Остановить приложение (чтобы не было активных подключений к БД)
docker compose stop backend

# 2) Пересоздать базу (чистое восстановление)
docker compose exec -T postgres psql -U "${DB_USER}" -d postgres <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};
SQL

# 3) Расшифровать, распаковать и залить SQL в PostgreSQL
export BACKUP_GPG_PASSPHRASE='ваш-passphrase'
gpg --batch --yes --passphrase-fd 0 --decrypt backups/sauda_prod_20250623_020000.sql.gz.gpg \
  | gunzip -c \
  | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "${DB_USER}" -d "${DB_NAME}" \
  <<< "${BACKUP_GPG_PASSPHRASE}"

# 4) Запустить приложение
docker compose start backend

# 5) Проверка
curl -s http://localhost/api/v1/health
docker compose exec postgres psql -U "${DB_USER}" -d "${DB_NAME}" -c '\dt'
```

#### 4. Восстановление с локальной машины (файл скачан с сервера)

```bash
# На своём компьютере (нужны docker, gpg, gunzip)
export BACKUP_GPG_PASSPHRASE='ваш-passphrase'
BACKUP=sauda_prod_20250623_020000.sql.gz.gpg

gpg --batch --yes --passphrase-fd 0 --decrypt "${BACKUP}" <<< "${BACKUP_GPG_PASSPHRASE}" \
  | gunzip -c \
  | docker compose -f docker-compose.yml exec -T postgres \
      psql -v ON_ERROR_STOP=1 -U sauda -d sauda_prod
```

#### 5. Только проверить, что файл читается (без restore в БД)

```bash
export BACKUP_GPG_PASSPHRASE='ваш-passphrase'
gpg --batch --yes --passphrase-fd 0 --decrypt backups/sauda_prod_20250623_020000.sql.gz.gpg \
  <<< "${BACKUP_GPG_PASSPHRASE}" \
  | gunzip -c \
  | head -50
```

## Secrets и переменные

| Имя | Тип | Назначение |
|-----|-----|------------|
| `DEPLOY_ENABLED` | Variable | `true` — включить CD и backup |
| `BACKUP_RETENTION_DAYS` | Variable | Сколько дней хранить `.sql.gz.gpg` на сервере (default: 14) |
| `BACKUP_GPG_PASSPHRASE` | Secret | Пароль для шифрования/расшифровки бэкапов (на уровне Environment) |
| `GHCR_USERNAME` | Secret | Login в GHCR |
| `GHCR_TOKEN` | Secret | PAT `write:packages` |
| `SERVER_HOST` | Secret | IP/домен сервера |
| `SERVER_USER` | Secret | SSH-пользователь |
| `SERVER_SSH_KEY` | Secret | Приватный SSH-ключ |

## GitHub Environments

| Environment | Когда | Approval |
|-------------|-------|----------|
| `dev` | Push `develop` | Не нужен |
| `test` | Push `release/*` | Не нужен |
| `production` | Ручной PROD | **Required reviewers** |

## Quality Gates

| Gate | Инструмент |
|------|------------|
| Статический анализ | Checkstyle |
| Форматирование | Spotless |
| Покрытие ≥70% | JaCoCo |
| Lint | ESLint |
| Типизация | TypeScript |

## Релиз в PROD

1. QA на TEST (`release/*` → merge после проверки)
2. Merge в `main` — **только CI**, prod не меняется
3. **Actions → Deploy PROD → Run workflow** (ref: `main`)
4. Approve в environment `production`
5. Проверка: `GET /api/v1/health` → `UP`
