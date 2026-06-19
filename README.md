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

GitFlow-подход:

- `main` — готовый к продакшену код
- `develop` — интеграционная ветка
- `feature/*` — новая функциональность
- `release/*` — стабилизация релиза
- `hotfix/*` — срочные исправления в проде

Прямые push в `main` запрещены. Все изменения — через Pull Request.

Подробнее: [docs/branching.md](docs/branching.md)

## Обзор CI/CD

### Continuous Integration

| Workflow | Триггеры | Проверки |
|----------|----------|----------|
| `backend-ci.yml` | PR, `develop`, `main` | Verify, тесты, JaCoCo (≥70%), Checkstyle, Spotless, JAR, Docker build |
| `frontend-ci.yml` | PR, `develop`, `main` | Lint, type check, build, тесты, Docker build |

Пайплайн падает при любой ошибке.

### Continuous Deployment

> **MVP:** деплой на сервер **отключён** по умолчанию (`DEPLOY_ENABLED` не задан).  
> CI работает без сервера. Чтобы включить CD — см. [docs/setup.md](docs/setup.md) и [infrastructure/deploy/README.md](infrastructure/deploy/README.md).

| Ветка / событие | Окружение | Workflow |
|-----------------|-----------|----------|
| `develop` | DEV | `deploy-dev.yml` (если `DEPLOY_ENABLED=true`) |
| `release/*` | TEST | `deploy-test.yml` (если `DEPLOY_ENABLED=true`) |
| `main` | — | **автодеплоя нет**, только CI |
| Ручной запуск | PROD | `deploy-prod.yml` + approval |

Шаги деплоя: сборка образа → push в GHCR → SSH на сервер → `docker compose pull` → `docker compose up -d`.

Необходимые секреты GitHub: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`, `GHCR_USERNAME`, `GHCR_TOKEN`.

Подробнее: [docs/cicd.md](docs/cicd.md)

## Quality Gates

- **Checkstyle** — статический анализ на этапе Maven `validate`
- **Spotless** — форматирование кода на этапе Maven `verify`
- **JaCoCo** — минимум 70% покрытия на этапе Maven `verify`

## Лицензия

Proprietary — платформа Sauda.
