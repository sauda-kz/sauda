# Архитектура Sauda

## Контекст системы

Sauda — B2B SaaS-платформа для небольших розничных магазинов. Владельцы магазинов сравнивают цены в каталогах поставщиков, анализируют разницу, собирают корзины и отправляют заказы.

Этот документ описывает **фундамент MVP-монолита**, спроектированный для эволюции в сторону микросервисов без преждевременного разделения.

## Высокоуровневая архитектура

```
                    ┌─────────────┐
                    │   Clients   │
                    │  (Browsers) │
                    └──────┬──────┘
                           │ HTTP
                    ┌──────▼──────┐
                    │    Nginx    │
                    │  (Reverse   │
                    │   Proxy)    │
                    └──┬───────┬──┘
                       │       │
              /api/*   │       │  /*
                       │       │
              ┌────────▼──┐ ┌──▼─────────┐
              │  Backend  │ │  Frontend  │
              │ Spring    │ │   React    │
              │ Boot API  │ │  SPA/Vite  │
              └─────┬─────┘ └────────────┘
                    │
              ┌─────▼─────┐
              │ PostgreSQL│
              └───────────┘
```

## Структура monorepo

| Модуль | Ответственность |
|--------|-----------------|
| `backend/sauda-api` | REST API, доменная логика, персистентность, интеграции |
| `frontend/sauda-web` | UI для магазинов |
| `infrastructure/` | Docker, Nginx, артефакты деплоя |
| `docs/` | Инженерная документация |

## Слои backend

```
controller  →  service  →  repository
                  ↓
            integration (внешние системы)
                  ↓
            infrastructure (адаптеры, messaging)
```

Пакеты под `com.sauda`:

| Пакет | Роль |
|-------|------|
| `config` | Конфигурация Spring |
| `common` | Общие константы и утилиты |
| `domain` | JPA-сущности и доменные модели |
| `dto` | Объекты запросов/ответов API |
| `repository` | Spring Data JPA репозитории |
| `service` | Бизнес-логика |
| `controller` | REST-эндпоинты |
| `exception` | Обработка ошибок |
| `integration` | Интерфейсы и реализации внешних провайдеров |
| `infrastructure` | Технические адаптеры (в будущем: messaging, cache) |

## Стратегия интеграций

Внешние системы доступны через интерфейсы с реализациями по профилям:

| Интерфейс | Mock (dev/test) | Real (prod) |
|-----------|-------------------------|-------------|
| `NotificationService` | `MockNotificationService` | `WhatsAppNotificationService` |
| `PaymentProvider` | `FakePaymentProvider` | `RealPaymentProvider` |
| `SupplierImportProvider` | `MockSupplierImportProvider` | `RealSupplierImportProvider` |

Spring `@Profile` выбирает активный bean. Прикладной код зависит только от интерфейсов.

## Управление данными

- **PostgreSQL** — основное хранилище
- **Flyway** — версионированные миграции схемы (`db/migration/`)
- **JPA/Hibernate** с `ddl-auto: validate` во всех окружениях, кроме тестов

## Безопасность (в будущем)

Spring Security запланирован, но в этом фундаменте не включён. Actuator предоставляет health-пробы для оркестрации контейнеров.

## Наблюдаемость

- Spring Boot Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- Кастомный health API: `/api/v1/health`
- Структурированное логирование с уровнями по профилям

## Модель деплоя

Все сервисы работают как Docker-контейнеры под управлением Docker Compose:

1. **nginx** — edge proxy
2. **frontend** — статика React, раздаётся через Nginx
3. **backend** — Spring Boot executable JAR
4. **postgres** — база данных

Образы собираются в CI, публикуются в GHCR и подтягиваются на целевые серверы при CD.

## Путь эволюции

Монолит структурирован для будущего выделения сервисов:

- Интерфейсы интеграций → anti-corruption layers для микросервисов
- Domain-пакеты → bounded contexts
- Infrastructure-адаптеры → event-driven границы

Разделение на микросервисы не выполняется, пока бизнес-границы и нагрузка этого не оправдывают.
