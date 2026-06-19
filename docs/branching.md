# Стратегия веток

GitFlow-подобная модель для MVP с тремя окружениями: **DEV → TEST → PROD**.

## Постоянные ветки

| Ветка | Назначение | Деплой |
|-------|------------|--------|
| `main` | Production-ready код | PROD (**только вручную**) |
| `develop` | Интеграция MVP | DEV (когда `DEPLOY_ENABLED=true`) |

## Вспомогательные ветки

| Шаблон | Из | В | Назначение |
|--------|-----|---|------------|
| `feature/*` | `develop` | `develop` | Фичи и задачи |
| `release/*` | `develop` | `main` + `develop` | Стабилизация релиза |
| `hotfix/*` | `main` | `main` + `develop` | Срочные исправления |

## Потоки

### Feature

```
feature/SAUDA-123
       │
       ▼
    develop  ──► DEV
```

### Release

```
develop
   │
   ▼
release/1.0.0  ──► TEST
   │
   ├──► main     (CI only, PROD вручную позже)
   └──► develop
```

### Hotfix

```
main
 │
 ▼
hotfix/1.0.1-fix
 │
 ├──► PR → main   (CI: Backend CI + Frontend CI)
 │         │
 │         ▼ merge
 │     Deploy PROD (вручную)
 │
 └──► back-merge PR main → develop  (создаётся автоматически)
           │
           ▼ merge
       develop синхронизирован
```

**Порядок действий:**

1. `git checkout main && git pull && git checkout -b hotfix/1.0.1-fix`
2. Исправление + push → CI на ветке `hotfix/**`
3. PR `hotfix/1.0.1-fix` → **`main`** — CI обязателен
4. Merge в `main` → CI на `main`
5. **Actions → Deploy PROD** (вручную)
6. GitHub Actions создаёт PR **`main` → `develop`** (workflow `back-merge-hotfix.yml`)
7. Merge back-merge PR в `develop` — CI на PR в `develop`

> Не удаляйте ветку `hotfix/*` до merge в `main` — иначе back-merge PR не создастся.

## Соответствие окружений

| Git-событие | Окружение | Автодеплой |
|-------------|-----------|------------|
| Push `develop` | DEV | Да* |
| Push `release/*` | TEST | Да* |
| Push `main` | — | **Нет** (только CI) |
| Run workflow Deploy PROD | PROD | Вручную + approval |

\* Только если в GitHub задано `DEPLOY_ENABLED=true`. Без сервера — пропускается с notice.

## Pull Request

- Нет прямых push в `main`
- CI обязателен: **Backend CI / Build, Test and Quality Gates** + **Frontend CI / Build, Test and Quality Gates**
- CI запускается на:
  - PR в `main` или `develop` (в т.ч. hotfix → main, back-merge → develop)
  - Push в `main`, `develop`, `hotfix/**`, `release/**`, `feature/**`
- Минимум 1 approval

## Именование

```
feature/SAUDA-123-cart-api
release/1.0.0
hotfix/1.0.1-payment-timeout
```

Подробнее: [cicd.md](cicd.md)
