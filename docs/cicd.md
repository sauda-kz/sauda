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

## Secrets и переменные

| Имя | Тип | Назначение |
|-----|-----|------------|
| `DEPLOY_ENABLED` | Variable | `true` — включить CD |
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
