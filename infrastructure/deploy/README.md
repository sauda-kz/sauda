# Деплой на сервер (заглушка для MVP)

Сейчас серверов нет — CD в GitHub Actions **отключён по умолчанию**.  
CI (сборка, тесты, Docker build) работает без сервера.

## Когда появится сервер

### 1. Подготовьте сервер

```bash
sudo mkdir -p /opt/sauda
cd /opt/sauda

# Скопируйте с репозитория:
# - docker-compose.yml
git clone git@github.com:YOUR_ORG/sauda.git .   # или scp файлов

cp infrastructure/deploy/server.env.example .env
# Отредактируйте .env — пароли, образы, профиль
```

Установите Docker, **GnuPG** (`sudo apt install -y gnupg`) и выполните `docker login ghcr.io`.

### 2. Настройте GitHub

**Secrets** (Settings → Secrets → Actions):

| Secret | Назначение |
|--------|------------|
| `GHCR_USERNAME` | GitHub login |
| `GHCR_TOKEN` | PAT с `write:packages` |
| `SERVER_HOST` | IP/домен сервера |
| `SERVER_USER` | SSH-пользователь |
| `SERVER_SSH_KEY` | Приватный SSH-ключ |
| `BACKUP_GPG_PASSPHRASE` | Пароль шифрования бэкапов PostgreSQL |

**Environments** (Settings → Environments):

| Environment | Когда | Approval |
|-------------|-------|----------|
| `dev` | Push в `develop` | Не нужен |
| `test` | Push в `release/*` | Не нужен |
| `production` | Ручной PROD | **Required reviewers** |

Для разных серверов задайте **отдельные secrets на уровне Environment** (не repository).

### 3. Включите деплой

**Settings → Secrets and variables → Actions → Variables**

| Variable | Value |
|----------|-------|
| `DEPLOY_ENABLED` | `true` |

После этого автоматически заработают:

- `develop` → DEV
- `release/*` → TEST

PROD — **всегда вручную**: Actions → **Deploy PROD** → Run workflow → approve.

### 4. Отключить деплой снова

Установите `DEPLOY_ENABLED=false` или удалите variable — workflow покажет notice и пропустит SSH.

## Как доставляются образы на сервер

По умолчанию (**transfer**) — сервер **не** качает из GHCR сам:

1. GitHub Actions собирает и пушит образы в GHCR
2. Runner скачивает образы и упаковывает в `.tar.gz`
3. SCP → `/tmp/sauda-deploy/` на сервер
4. `remote-deploy.sh` делает `docker load` и `docker compose up`

Это решает ошибки вида `dial tcp ... ghcr.io ... i/o timeout` и `TLS handshake timeout` — типично, если VPS нестабильно ходит на GitHub.

### Если сервер стабильно видит GHCR

**Settings → Actions → Variables:**

| Variable | Value |
|----------|-------|
| `DEPLOY_PULL_FROM_GHCR` | `true` |

Тогда сервер сам делает `docker compose pull` (нужен `docker login ghcr.io` — credentials передаются из secrets).

### Перед первым деплоим с transfer

На сервере обновите репозиторий (нужен `remote-deploy.sh`):

```bash
cd /opt/sauda && git pull
chmod +x infrastructure/deploy/remote-deploy.sh
```

### Диагностика на сервере

```bash
# Проверка доступа к GHCR с сервера
curl -I --connect-timeout 10 https://ghcr.io/v2/

# Если timeout — оставьте transfer (DEPLOY_PULL_FROM_GHCR не задавать)
```

## Окружения

| Окружение | Ветка | Триггер | Профиль Spring |
|-----------|-------|---------|----------------|
| DEV | `develop` | auto (если DEPLOY_ENABLED) | `dev` |
| TEST | `release/*` | auto (если DEPLOY_ENABLED) | `test` |
| PROD | `main` | **только вручную** | `prod` |

Push в `main` **не деплоит** никуда — только CI.

## Примеры .env на сервере

**DEV** (`/opt/sauda/.env`):

```env
BACKEND_IMAGE=ghcr.io/org/sauda/sauda-api:dev-latest
FRONTEND_IMAGE=ghcr.io/org/sauda/sauda-web:dev-latest
SPRING_PROFILES_ACTIVE=dev
DB_PASSWORD=...
```

**PROD** (отдельный сервер или тот же с другим .env):

```env
BACKEND_IMAGE=ghcr.io/org/sauda/sauda-api:prod-latest
FRONTEND_IMAGE=ghcr.io/org/sauda/sauda-web:prod-latest
SPRING_PROFILES_ACTIVE=prod
WHATSAPP_API_URL=https://...
```

Полная инструкция: [docs/setup.md](../../docs/setup.md)
