# Sauda

Sauda — B2B-платформа, которая помогает небольшим магазинам сравнивать цены поставщиков, анализировать разницу, формировать корзины и отправлять заказы.

Этот репозиторий содержит **инженерный фундамент** для MVP Sauda: monorepo, каркас Spring Boot API, React-фронтенд, Docker-стек, стратегию веток и CI/CD.

> Подробная инструкция по настройке реальных данных и GitHub: [docs/setup.md](docs/setup.md)

## Технологический стек

| Слой | Технологии |
|------|------------|
| Backend | Java 21, Spring Boot 3.x, Maven, PostgreSQL, Flyway, Spring Data JPA, Lombok, Actuator |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Инфраструктура | Docker, Docker Compose, Nginx, GitHub Actions, GHCR |

## Структура репозитория

```
sauda/
├── backend/sauda-api/          # Spring Boot REST API
├── frontend/sauda-web/         # React SPA (Vite)
├── infrastructure/
│   ├── docker/                 # Dockerfiles
│   └── nginx/                  # Конфигурация reverse proxy
├── docs/                       # Документация
├── .github/workflows/          # CI/CD пайплайны
└── docker-compose.yml          # Локальный и серверный стек
```

## Обзор архитектуры

- **Monorepo** с чётким разделением backend / frontend / infrastructure
- **Интеграции по профилям** — mock в `dev`/`test`, реальные провайдеры в `prod`
- **Nginx** принимает HTTP и направляет `/api` и `/actuator` на backend, остальной трафик — на frontend
- **PostgreSQL** — основное хранилище; схема управляется через Flyway

Подробнее: [docs/architecture.md](docs/architecture.md)

## Локальный запуск

### Требования

- Java 21
- Node.js 22+
- Docker и Docker Compose
- PostgreSQL 16 (опционально, если используете только Docker)

### Backend

```bash
cd backend/sauda-api
./mvnw spring-boot:run
```

Health API: `http://localhost:8080/api/v1/health`  
Actuator: `http://localhost:8080/actuator/health`

### Frontend

```bash
cd frontend/sauda-web
npm install
npm run dev
```

Приложение: `http://localhost:3000`

## Запуск через Docker

1. Скопируйте шаблон окружения:

```bash
cp .env.example .env
```

2. Запустите весь стек:

```bash
docker compose up -d --build
```

3. Доступ к приложению:

- Web UI: `http://localhost`
- API: `http://localhost/api/v1/health`
- Actuator: `http://localhost/actuator/health`

## Стратегия окружений

Поведение задаётся Spring-профилями:

| Профиль | Назначение | Интеграции |
|---------|------------|------------|
| `dev` | Локальная разработка | Mock |
| `test` | QA / изолированное тестирование | Mock |
| `prod` | Продакшен | Реальные |

Активация: `SPRING_PROFILES_ACTIVE=dev|test|prod`

Подробнее: [docs/environments.md](docs/environments.md)

## Стратегия веток

GitFlow-подход для MVP:

| Ветка | Назначение |
|-------|------------|
| `main` | Код, готовый к продакшену. **Автодеплоя нет** — только CI |
| `develop` | Интеграция текущей работы над MVP |
| `feature/*` | Новая функциональность (задачи, фичи) |
| `release/*` | Стабилизация релиза перед QA |
| `hotfix/*` | Срочные исправления в проде |

Прямые push в `main` запрещены. Все изменения — через Pull Request.

Подробнее: [docs/branching.md](docs/branching.md)

---

## Как доставляется новая функциональность

Полный путь фичи от разработки до PROD:

```
┌─────────────────────────────────────────────────────────────────┐
│  1. РАЗРАБОТКА                                                  │
│     feature/SAUDA-123-cart  ←  ветка от develop                 │
│     Локально: mvnw / npm run dev  или  docker compose up        │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Pull Request
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. CI (автоматически на каждый PR)                             │
│     backend-ci.yml  → тесты, coverage, Checkstyle, Spotless    │
│     frontend-ci.yml → lint, type-check, build, tests            │
│     Образы собираются, но на сервер НЕ деплоятся                │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Merge PR → develop
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. DEV (если DEPLOY_ENABLED=true)                              │
│     deploy-dev.yml → GHCR → SSH → docker compose up             │
│     Профиль Spring: dev, mock-интеграции                        │
└───────────────────────────────┬─────────────────────────────────┘
                                │ release/1.0.0 от develop
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. TEST (если DEPLOY_ENABLED=true)                             │
│     deploy-test.yml → QA проверяет на test-сервере              │
│     Профиль Spring: test, mock-интеграции                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Merge release → main
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. main — только CI, prod НЕ меняется                          │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Actions → Deploy PROD (вручную)
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. PROD (ручной запуск + approval)                             │
│     deploy-prod.yml → prod-сервер, реальные интеграции          │
└─────────────────────────────────────────────────────────────────┘
```

### Пример: добавили API корзины

```bash
# 1. Новая ветка от develop
git checkout develop && git pull
git checkout -b feature/SAUDA-045-cart-api

# 2. Разработка + локальные тесты
cd backend/sauda-api && ./mvnw verify
cd frontend/sauda-web && npm test && npm run build

# 3. Push и PR в develop (не в main!)
git push -u origin feature/SAUDA-045-cart-api
# → GitHub: Pull Request → develop
# → CI должен пройти → code review → merge

# 4. После merge в develop — автодеплой на DEV (когда CD включён)
```

### Что происходит на каждом этапе

| Этап | Кто запускает | Что проверяется | Деплой на сервер |
|------|---------------|-----------------|------------------|
| PR в `develop` | Авто (CI) | Код, тесты, quality gates | Нет |
| Merge в `develop` | Авто (CD*) | — | DEV |
| Push `release/*` | Авто (CD*) | QA на TEST | TEST |
| Merge в `main` | Авто (CI) | Сборка релиза | **Нет** |
| Deploy PROD | **Вручную** | Approval в GitHub | PROD |

\* CD работает только если `DEPLOY_ENABLED=true` (см. ниже).

---

## CI/CD: как это устроено

### Continuous Integration (работает уже сейчас)

CI **не требует сервера** — запускается на GitHub runners.

| Workflow | Когда | Что делает |
|----------|-------|------------|
| `backend-ci.yml` | PR, push в `develop` / `main` | Maven verify, JaCoCo ≥70%, Checkstyle, Spotless, сборка JAR и Docker-образа |
| `frontend-ci.yml` | PR, push в `develop` / `main` | ESLint, TypeScript, Vitest, `vite build`, сборка Docker-образа |

При ошибке на любом шаге — **pipeline падает**, merge блокируется (если настроен branch protection).

### Continuous Deployment (заглушка для MVP)

> **Сейчас:** CD **отключён по умолчанию**. Workflow запускается, но вместо SSH выводит notice «Deploy disabled (MVP)» и **ничего не ломает**.

| Workflow | Триггер | Окружение | Профиль |
|----------|---------|-----------|---------|
| `deploy-dev.yml` | Push в `develop` | DEV | `dev` |
| `deploy-test.yml` | Push в `release/*` | TEST | `test` |
| `deploy-prod.yml` | **Только вручную** (Actions → Run workflow) | PROD | `prod` |

Push в `main` **не деплоит** — только прогоняет CI.

#### Что делает deploy-workflow (когда включён)

```
1. Checkout кода
2. docker build → push образов в GHCR
      ghcr.io/<org>/<repo>/sauda-api:dev-latest
      ghcr.io/<org>/<repo>/sauda-web:dev-latest
3. SSH на сервер (/opt/sauda)
4. docker compose pull
5. docker compose up -d
```

---

## Как включить реальный деплой (чеклист)

Когда появится сервер — выполните **все** пункты. Без любого из них деплой не сработает.

### Шаг 1. Сервер

```bash
sudo mkdir -p /opt/sauda && cd /opt/sauda
git clone git@github.com:YOUR_ORG/sauda.git .
cp infrastructure/deploy/server.env.example .env
nano .env   # пароли, образы, профиль
```

На сервере должны быть:
- Docker + Docker Compose plugin
- `docker login ghcr.io` (PAT с `read:packages`)
- Открыт SSH для GitHub Actions
- Файлы: `docker-compose.yml`, `.env`, `infrastructure/nginx/nginx.conf`

Шаблон `.env`: [infrastructure/deploy/server.env.example](infrastructure/deploy/server.env.example)

### Шаг 2. GitHub Secrets

**Settings → Secrets and variables → Actions → Repository secrets**

| Secret | Значение | Зачем |
|--------|----------|-------|
| `GHCR_USERNAME` | Ваш GitHub login | Push образов в registry |
| `GHCR_TOKEN` | PAT (`write:packages`, `read:packages`) | Авторизация в GHCR |
| `SERVER_HOST` | IP или домен сервера | SSH-подключение |
| `SERVER_USER` | SSH-пользователь, напр. `deploy` | SSH-подключение |
| `SERVER_SSH_KEY` | Приватный ключ целиком | SSH-подключение |

> Для **разных серверов** DEV / TEST / PROD — задайте secrets **на уровне Environment**, а не repository.

### Шаг 3. GitHub Environments

**Settings → Environments**

| Environment | Когда используется | Approval |
|-------------|-------------------|----------|
| `dev` | Push в `develop` | Не нужен |
| `test` | Push в `release/*` | Не нужен |
| `production` | Ручной Deploy PROD | **Required reviewers** — обязательно |

### Шаг 4. Включить CD (главный переключатель)

**Settings → Secrets and variables → Actions → Variables**

| Variable | Value |
|----------|-------|
| `DEPLOY_ENABLED` | `true` |

До этого шага deploy-workflows **безопасно пропускают** деплой.

### Шаг 5. Branch protection

**Settings → Branches**

- `main` и `develop`: require PR, require CI checks (Backend CI + Frontend CI)

### Шаг 6. Проверка

```bash
# После merge в develop — смотрите Actions → Deploy DEV
# На сервере:
docker ps
curl http://SERVER_HOST/api/v1/health
```

---

## Состояния CD (шпаргалка)

| `DEPLOY_ENABLED` | Secrets | Сервер | Результат |
|------------------|---------|--------|-----------|
| не задан / `false` | — | — | CI ✅, CD ⏸ (notice, без ошибок) |
| `true` | ❌ нет | — | Workflow **упадёт** на login/SSH |
| `true` | ✅ есть | ❌ нет | Workflow **упадёт** на SSH |
| `true` | ✅ есть | ✅ готов | **Реальный деплой** ✅ |

---

## PROD-релиз (когда CD включён)

1. QA прошёл на TEST (`release/1.0.0`)
2. Merge `release/*` → `main` (prod **ещё не обновился**)
3. **Actions → Deploy PROD → Run workflow**
   - `git_ref`: `main`
4. Подтвердите deployment в environment `production`
5. Проверка: `GET /api/v1/health` → `{"status":"UP",...}`

---

## Дополнительная документация

| Документ | Содержание |
|----------|------------|
| [docs/setup.md](docs/setup.md) | Настройка GitHub и сервера |
| [docs/cicd.md](docs/cicd.md) | Детали пайплайнов |
| [docs/branching.md](docs/branching.md) | Стратегия веток |
| [docs/environments.md](docs/environments.md) | Профили Spring и переменные |
| [infrastructure/deploy/README.md](infrastructure/deploy/README.md) | Шаблон сервера |

---

## Quality Gates

- **Checkstyle** — статический анализ на этапе Maven `validate`
- **Spotless** — форматирование кода на этапе Maven `verify`
- **JaCoCo** — минимум 70% покрытия на этапе Maven `verify`

## Лицензия

Proprietary — платформа Sauda.
