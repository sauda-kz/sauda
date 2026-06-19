# Sauda Web

React SPA для B2B-платформы Sauda.

## Стек

- React 19
- TypeScript
- Vite 6
- Tailwind CSS 4
- Vitest

## Разработка

```bash
npm install
npm run dev
```

Приложение: http://localhost:3000

Запросы к `/api` и `/actuator` проксируются на `http://localhost:8080` при локальной разработке.

## Скрипты

| Команда | Описание |
|---------|----------|
| `npm run dev` | Dev-сервер Vite |
| `npm run build` | Production-сборка в `dist/` |
| `npm run preview` | Просмотр production-сборки |
| `npm run lint` | ESLint |
| `npm run type-check` | Проверка TypeScript |
| `npm test` | Unit-тесты Vitest |

## Переменные окружения

| Переменная | Описание |
|------------|----------|
| `VITE_API_URL` | Базовый URL API (по умолчанию `/api`) |

Vite встраивает `VITE_*` переменные **на этапе сборки**. Для production-образов задавайте их в `.env` или Docker build args.
