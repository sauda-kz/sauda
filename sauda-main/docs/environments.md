# Стратегия окружений

Sauda использует Spring-профили для настройки поведения в разных окружениях без изменения кода.

## Профили

| Профиль | Конфиг Spring | База данных | Интеграции | Логирование |
|---------|---------------|-------------|------------|-------------|
| `dev` | `application-dev.yml` | Локальный PostgreSQL | Mock | DEBUG для `com.sauda` |
| `test` | `application-test.yml` | Изолированная test DB | Mock | INFO |
| `prod` | `application-prod.yml` | Production PostgreSQL | Реальные | WARN root, INFO app |

Базовые настройки — в `application.yml`. Профильные файлы переопределяют datasource, logging и URL интеграций.

## Активация

### Локальная разработка

```bash
export SPRING_PROFILES_ACTIVE=dev
cd backend/sauda-api
./mvnw spring-boot:run
```

### Docker Compose

```env
SPRING_PROFILES_ACTIVE=dev
```

### CI/CD (когда сервер готов)

| Деплой | Профиль |
|--------|---------|
| DEV | `dev` |
| TEST | `test` |
| PROD | `prod` |

## Переменные конфигурации

### База данных

| Переменная | Описание | Default (dev) |
|------------|----------|---------------|
| `DB_HOST` | Хост PostgreSQL | `localhost` |
| `DB_PORT` | Порт | `5432` |
| `DB_NAME` | Имя базы | `sauda_dev` |
| `DB_USER` | Пользователь | `sauda` |
| `DB_PASSWORD` | Пароль | `sauda` |

### URL интеграций (prod)

| Переменная | Используется в |
|------------|----------------|
| `WHATSAPP_API_URL` | `WhatsAppNotificationService` |
| `PAYMENT_PROVIDER_URL` | `RealPaymentProvider` |
| `SUPPLIER_IMPORT_URL` | `RealSupplierImportProvider` |

## Поведение интеграций

### DEV / TEST

Mock-реализации (логирование, fake-транзакции, тестовые SKU).

### PROD

Реальные провайдеры WhatsApp, платежей и импорта поставщиков.

## Frontend

| Переменная | Назначение |
|------------|------------|
| `VITE_API_URL` | Базовый URL API (сборка Vite) |

Локально: `/api` (прокси Vite или Nginx).

## Секреты

- **Локально:** `.env` (не коммитить)
- **CI/CD:** GitHub Secrets
- **Сервер:** `/opt/sauda/.env` — шаблон: `infrastructure/deploy/server.env.example`

## Тесты

In-memory H2, Flyway отключён. Overrides: `src/test/resources/application-dev.yml`.
