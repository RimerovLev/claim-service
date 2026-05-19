# claims-mvp

AI-агент для получения компенсаций от авиакомпаний. Принимает заявки пассажиров на компенсацию за задержки/отмены рейсов и проблемы с багажом, автоматически рассчитывает право на компенсацию по EU 261/2004 и Montreal Convention 1999, генерирует претензионное письмо, ведёт claim через FSM-воронку и шлёт уведомления.

**Статус:** MVP+. Backend закрыт (все 6 типов кейсов, security, scheduler). Frontend — скелет SPA. До деплоя ~3 спринта (см. `docs/roadmap.md`).

---

## Что умеет

- 6 типов кейсов end-to-end: задержка рейса, отмена, missed connection, задержка/потеря/повреждение багажа.
- Pure rule-based eligibility engine (EU 261 + Montreal Convention).
- Генерация претензионного письма по типу кейса.
- FSM жизненного цикла claim с аудит-логом каждого шага.
- Загрузка документов с защитой от MIME-spoofing и path traversal.
- Email-уведомления пользователю при событиях (event-driven, AFTER_COMMIT).
- Автоматический follow-up для submitted-claim'ов через 14 дней (cron 9:00 ежедневно).
- Stateless JWT-аутентификация, 3 роли (USER, MODERATOR, ADMIN), `@PreAuthorize` + ownership-checks.
- Admin API для управления ролями.

## Стек

**Backend:** Java 21, Spring Boot 4, PostgreSQL 16, Flyway, JPA/Hibernate, MapStruct, Lombok, Spring Security + JJWT, Spring Mail, Jackson 3.

**Frontend:** React 19, Vite, TypeScript, React Router v7.

**Тесты:** JUnit 5, Mockito, TestContainers (Postgres), Spring Boot Test (`@WebMvcTest`, `@SpringBootTest`).

## Структура

```
claims-mvp/
├── src/                     Spring Boot backend
│   ├── main/java/com/claims/mvp/
│   │   ├── claim/           orchestrator + FSM + eligibility + letter + storage
│   │   ├── eligibility/     pure rule engine (стратегии per IssueType)
│   │   ├── notifications/   event-driven email + Application Events
│   │   ├── scheduler/       cron-задачи (follow-up)
│   │   ├── security/        JWT, filters, auth controller
│   │   ├── user/            пользователи, admin endpoints
│   │   └── events/          ClaimEvents аудит-лог
│   └── main/resources/db/migration/   Flyway V1-V6
├── frontend/                React SPA (Vite + TS)
│   └── src/
│       ├── api/             fetch-обёртки с JWT
│       ├── auth/            сессия + защищённые роуты
│       ├── components/      Sidebar
│       └── pages/           Dashboard / Claims / NewClaim / ...
├── docs/                    архитектура, roadmap, daily snapshots
├── uploads/                 локальное хранилище документов (dev)
├── pom.xml
└── mvnw / mvnw.cmd
```

## Запуск локально

**Требования:** Java 21, Docker (для Postgres и MailHog), Node.js 20+ (для frontend).

```bash
# 1. Postgres + MailHog
docker run -d --name claims-postgres \
  -e POSTGRES_DB=claims_mvp \
  -e POSTGRES_USER=claims_user \
  -e POSTGRES_PASSWORD=claims_pass \
  -p 5432:5432 postgres:16

docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog

# 2. Backend (применит миграции Flyway автоматически)
./mvnw spring-boot:run

# 3. Frontend (в отдельном терминале)
cd frontend
npm install
npm run dev
```

После старта:
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- MailHog UI: `http://localhost:8025` (увидеть отправленные письма)
- Thymeleaf home page: `http://localhost:8080/`

## Тесты

```bash
./mvnw clean test
```

В тестах:
- Production `SecurityConfig` отключается через `app.security.enabled=false` (`IntegrationTestBase`).
- `TestSecurityConfig` подменяет permit-all chain и провайдит синтетического test-юзера.
- `JavaMailSender` мокается, реальные SMTP-запросы не идут.

## Документация

| Файл | Что |
|------|-----|
| [docs/architecture-overview.md](docs/architecture-overview.md) | Карта проекта: классы, ответственности, потоки, security |
| [docs/roadmap.md](docs/roadmap.md) | План развития до полной реализации ТЗ |
| [docs/tz-checklist.md](docs/tz-checklist.md) | Что из ТЗ сделано / не сделано |
| [docs/week-plan.md](docs/week-plan.md) | Текущий спринт |
| [docs/daily/](docs/daily/) | Snapshot'ы дней с решениями и силент-багами |
| [CLAUDE.md](CLAUDE.md) | Правила и known issues для агента/нового разработчика |

## Архитектурные принципы

1. **`ClaimLifecycleServiceImpl`** — единственный orchestrator. Контроллеры не вызывают другие сервисы напрямую.
2. **`ClaimWorkflowServiceImpl`** — единственное место где живёт `ClaimStatus`. FSM-переходы валидируются здесь.
3. **`EligibilityServiceImpl`** — pure rule engine, ноль I/O. Делегирует в стратегии per `IssueType`.
4. **Добавление нового типа кейса = новая стратегия + `@Component`.** Сервисы не трогаются — Spring подхватывает через `List<Strategy>`-инъекцию.
5. **Notifications через domain events** (`ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`). Lifecycle не знает об email/SMS.
6. **Migrations только через Flyway.** Никаких `ddl-auto=create/update` в проде.
7. **Ownership-проверка в сервисном слое** (`assertOwnerOrPrivileged`). `@PreAuthorize` отвечает за роли, ownership — рантайм-проверка.
8. **Все scheduled-методы** выставляют synthetic `SecurityContext` перед вызовом защищённых сервисов.

## Известные ограничения

- Outbound email уходит только пользователю, не в авиакомпанию (Sprint B приоритет).
- Frontend без auth-интеграции в продовой версии (Sprint A).
- Локальная файловая система для документов — не S3. Перед scale мигрировать.
- `ClaimEvents.payload` — `TEXT`, не `jsonb`. Аналитика будет медленной.
- `Claim @OneToOne` ассоциации фактически EAGER (Hibernate-ограничение).

## Frontend

```bash
cd frontend
npm run dev      # dev server (localhost:5173 with HMR)
npm run build    # production build → dist/
npm run preview  # preview built artifact
npm run lint     # eslint
```

Vite-proxy для `/api/*` нужен — иначе frontend стучится в свой origin (5173). Настроить в `vite.config.ts`:

```ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

## Лицензия

Учебный проект. Не для коммерческого использования без отдельной договорённости.
