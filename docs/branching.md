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
main → hotfix/1.0.1 → main → PROD (вручную) → develop
```

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
- CI обязателен: Backend CI + Frontend CI
- Минимум 1 approval

## Именование

```
feature/SAUDA-123-cart-api
release/1.0.0
hotfix/1.0.1-payment-timeout
```

Подробнее: [cicd.md](cicd.md)
