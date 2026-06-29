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
npm run dev          # API через удалённый сервер (194.238.41.47)
npm run dev:local    # API через локальный backend (localhost:8080)
```

Приложение: http://localhost:3000

Запросы к `/api` и `/actuator` проксируются на backend, заданный в `DEV_PROXY_TARGET` (см. `.env.remote` / `.env.localdev`).

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
| `DEV_PROXY_TARGET` | Backend для Vite proxy в dev (только dev-сервер) |
| `VITE_API_URL` | Базовый URL API в браузере (по умолчанию `/api`) |

Профили: `.env.remote` (сервер) и `.env.localdev` (localhost). Можно переопределить через свой `.env.remote.local` или `.env.localdev.local`.

Vite встраивает `VITE_*` переменные **на этапе сборки**. Для production-образов задавайте их в `.env` или Docker build args.
