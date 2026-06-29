# Настройка проекта: реальные данные и GitHub

Пошаговая инструкция для MVP Sauda.

---

## MVP: сервера пока нет — это нормально

| Что | Статус сейчас | Что делать |
|-----|---------------|------------|
| **CI** (тесты, сборка) | ✅ Работает без сервера | Ничего |
| **CD** (деплой по SSH) | ⏸ Отключён | Не настраивать secrets пока не нужно |
| **Локальный Docker** | ✅ `docker compose up` | Для разработки на своей машине |

CD включается **одной переменной** в GitHub, когда сервер будет готов:

**Settings → Actions → Variables → `DEPLOY_ENABLED` = `true`**

Шаблон сервера: [infrastructure/deploy/server.env.example](../infrastructure/deploy/server.env.example)

---

## 1. Локальная разработка (сейчас)

### 1.1. Файл `.env` (корень репозитория)

```bash
cp .env.example .env
```

| Переменная | Что указать |
|------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `DB_PASSWORD` | Любой пароль для локальной БД |
| `VITE_API_URL` | `/api` — не менять для Docker |

### 1.2. Запуск

```bash
# Весь стек
docker compose up -d --build

# Или по отдельности
cd backend/sauda-api && ./mvnw spring-boot:run
cd frontend/sauda-web && npm install && npm run dev
```

**Запуск API из IntelliJ / Maven на хосте:** поднимите только Postgres и пробросьте порт на `localhost`:

```bash
docker compose up -d postgres
```

В `docker-compose.yml` для `postgres` опубликован порт `5432` (`DB_PUBLISH_PORT`). Профиль `dev` подключается к `localhost:5432` с учётками из `.env.example` (`sauda` / `sauda`, БД `sauda_dev`).

---

## 2. GitHub (минимум для MVP без сервера)

1. Создайте репозиторий, запушьте код
2. Ветки: `main` + `develop`
3. **Branch protection** на `main` и `develop` + required CI checks
4. **Secrets и CD пока не нужны**

CI запустится на первом PR/push автоматически.

---

## 3. Когда появится сервер

### 3.1. Подготовка сервера

```bash
sudo mkdir -p /opt/sauda && cd /opt/sauda
git clone git@github.com:YOUR_ORG/sauda.git .
cp infrastructure/deploy/server.env.example .env
# отредактируйте .env
docker login ghcr.io
```

### 3.2. GitHub Secrets

| Secret | Назначение |
|--------|------------|
| `GHCR_USERNAME` | GitHub login |
| `GHCR_TOKEN` | PAT: `write:packages`, `read:packages` |
| `SERVER_HOST` | IP или домен |
| `SERVER_USER` | SSH-пользователь |
| `SERVER_SSH_KEY` | Приватный ключ |

Для DEV/TEST/PROD на **разных серверах** — задайте secrets **на уровне Environment**, а не repository.

### 3.3. GitHub Environments

| Environment | Когда |
|-------------|-------|
| `dev` | Push в `develop` |
| `test` | Push в `release/*` |
| `production` | Ручной PROD (**Required reviewers**) |

### 3.4. Включить деплой

```
DEPLOY_ENABLED = true
```

После этого:

- `develop` → DEV (авто)
- `release/*` → TEST (авто)
- PROD → **Actions → Deploy PROD → Run workflow** + approval

**Push в `main` не деплоит никуда** — только CI.

---

## 4. Окружения

| Окружение | Профиль Spring | Триггер |
|-----------|----------------|---------|
| DEV | `dev` | Push `develop` |
| TEST | `test` | Push `release/*` |
| PROD | `prod` | Ручной workflow |

Staging **убран** — три окружения достаточно для MVP.

---

## 5. PROD-релиз (когда сервер готов)

1. QA на TEST (`release/1.0.0`)
2. Merge в `main` — prod **не меняется**
3. **Actions → Deploy PROD** → ref: `main` → Run
4. Approve в `production`
5. `curl https://your-domain/api/v1/health`

---

## 6. Чеклист

### Сейчас (MVP без сервера)

- [ ] Репозиторий на GitHub
- [ ] Ветки `main`, `develop`
- [ ] Branch protection + CI checks
- [ ] Локально: `docker compose up` работает

### Позже (когда будет сервер)

- [ ] Secrets в GitHub
- [ ] Environments: `dev`, `test`, `production`
- [ ] `/opt/sauda` на сервере + `.env`
- [ ] `DEPLOY_ENABLED=true`
- [ ] Первый деплой DEV с `develop`

---

## 7. Карта файлов

| Файл | Когда менять |
|------|--------------|
| `.env` (локально) | Локальная разработка |
| `infrastructure/deploy/server.env.example` | Шаблон для сервера |
| `/opt/sauda/.env` | Реальные данные на сервере |
| GitHub Variable `DEPLOY_ENABLED` | Включить/выключить CD |
| GitHub Secrets | Credentials для GHCR и SSH |
