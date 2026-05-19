# Архитектура claims-mvp

Состояние на 2026-05-10.

## Что делает приложение

Сервис принимает заявки от пассажиров на компенсацию за задержки/отмены рейсов и проблемы с багажом под EU 261/2004 и Montreal Convention 1999. Пассажир (или оператор студии) создаёт claim с данными рейса и инцидента, прикладывает документы. Система рассчитывает eligibility и сумму, генерирует претензионное письмо, ведёт claim через FSM-воронку с аудит-логом, шлёт email-уведомления при создании и переходах, и автоматически продвигает submitted-claim'ы в follow-up через 14 дней (cron 9:00 ежедневно).

Доступ под Spring Security + JWT. Три роли: USER (пассажир), MODERATOR (оператор студии), ADMIN.

## Стек

- **Java 21** + **Spring Boot 4** (`@MockitoBean`, не deprecated `@MockBean`).
- **PostgreSQL 16** + **Flyway** (V1-V6).
- **JPA / Hibernate** + **MapStruct** (DTO ↔ entity).
- **Lombok** (`@Getter/@Setter/@RequiredArgsConstructor/@Slf4j`).
- **Spring Security** + **JJWT** (HS256 stateless tokens).
- **Spring Mail** (`JavaMailSender`, dev: MailHog, prod: SMTP env vars).
- **`@TransactionalEventListener(AFTER_COMMIT)`** — eventual consistency для нотификаций.
- **`@Scheduled`** cron — follow-up автоматизация.
- **TestContainers** (Postgres) + **JUnit 5** + **Mockito**.
- **Jackson 3** (`tools.jackson.databind.ObjectMapper`, не `com.fasterxml`).
- **Frontend:** Vite + React 19 + TypeScript + React Router v7.

## Данные

```
User (id, fullName, email, passwordHash, role, createdAt)
 └── Claim (один user → много claims)
      ├── Flight (1:1)             flightNumber, airline, flightDate, route, distanceKm, bookingRef
      ├── Issue (1:1)               type + поля под тип (см. ниже)
      ├── EuContext (1:1)           departureFromEu, euCarrier
      ├── BoardingDocuments[]       TICKET, BOARDING_PASS, BAG_TAG, PIR, PHOTO
      └── ClaimEvents[]             аудит-лог (тип события + JSON payload)
```

`Issue.type` определяет какие поля заполнены:

| IssueType | Используемые поля |
|-----------|-------------------|
| `DELAY` | `delayMinutes` |
| `CANCELLATION` | `cancellationNoticeDays` |
| `MISSED_CONNECTION` | `delayMinutes` (итоговая задержка прибытия) |
| `BAGGAGE_DELAYED` | `baggageDelayHours` |
| `BAGGAGE_LOST` | `baggageDelayHours` (>504ч = 21 день = lost) |
| `BAGGAGE_DAMAGED` | `daysSinceDelivery` |

`Claim` хранит **derived поля** — `eligible`, `compensationAmount`, `status`. Пересчитываются при каждом create/update через `recalcDerivedFields`.

`ClaimStatus` (FSM): `NEW → DOCS_REQUESTED ⇄ READY_TO_SUBMIT → SUBMITTED → FOLLOW_UP_SENT → ESCALATED → APPROVED/REJECTED → PAID/CLOSED`.

**Миграции Flyway:**
- V1 — базовая схема (users, claims, flights, issues, eu_context, documents, claim_events).
- V2 — `baggage_delay_hours` в `issues`.
- V3 — `days_since_delivery` в `issues`.
- V4 — `password_hash` в `users`.
- V5 — `role` в `users`.
- V6 — seed admin user.

## Пакеты — что где

```
com.claims.mvp/
├── claim/
│   ├── controller/           ClaimController, DocumentController
│   ├── service/
│   │   ├── lifecycle/        ClaimLifecycleServiceImpl       — оркестратор
│   │   ├── workflow/         ClaimWorkflowServiceImpl        — FSM
│   │   ├── documents/        ClaimDocumentsServiceImpl       — merge документов в claim
│   │   ├── storage/          DocumentStorageServiceImpl      — диск + MIME-валидация
│   │   └── letter/
│   │       ├── ClaimLetterServiceImpl                         — делегатор
│   │       └── strategy/     LetterStrategy + 6 реализаций
│   ├── dao/                  ClaimRepository, BoardingDocumentsRepository
│   ├── dto/request|response/
│   ├── mapper/               ClaimMapper, ClaimEntityMapper, DocumentMapper (MapStruct)
│   ├── model/                Claim, Flight, Issue, EuContext, BoardingDocuments
│   └── enums/                ClaimStatus, IssueType, DocumentTypes, EventTypes
│
├── eligibility/
│   ├── service/              EligibilityServiceImpl          — делегатор (pure)
│   ├── strategy/             EligibilityStrategy + 6 реализаций
│   └── dto/response/         EligibilityResult
│
├── notifications/
│   ├── NotificationService                                    — интерфейс
│   ├── EmailNotificationService                               — JavaMailSender + listeners
│   └── events/
│       ├── ClaimCreatedEvent                                  — record(Claim)
│       └── ClaimStatusTransitionedEvent                       — record(Claim, from, to)
│
├── scheduler/
│   ├── FollowUpSchedulerService                               — @Scheduled cron 9:00
│   └── controller/           SchedulerAdminController         — manual trigger (dev only)
│
├── security/
│   ├── SecurityConfig                                         — JWT chain + @EnableMethodSecurity
│   ├── JwtAuthFilter                                          — извлекает токен, ставит SecurityContext
│   ├── JwtService                                             — generate/parse/validate
│   ├── JwtAuthentication                                      — кастомный Authentication
│   ├── controller/           AuthController                   — /api/auth/register, /login
│   ├── service/              AuthServiceImpl
│   └── dto/                  RegisterRequest, LoginRequest, AuthResponse
│
├── events/                   ClaimEvents аудит-лог
│   ├── dao/                  EventsRepository (+ findClaimIdsEligibleForFollowUp)
│   ├── model/                ClaimEvents
│   └── dto/response/         EventsResponse
│
├── user/
│   ├── controller/           UserController, AdminUserController
│   ├── service/              UserServiceImpl
│   ├── dao/                  UserRepository (findByEmail)
│   ├── model/                User, Role
│   ├── mapper/               UserMapper
│   └── dto/                  CreateUserRequest, ChangeRoleRequest, UserResponse
│
├── exception/                GlobalExceptionHandler, DuplicateUserException
└── web/                      HomeController (Thymeleaf /)
```

**Frontend** (`/frontend/`):
```
src/
├── App.tsx                   Router (Dashboard, Claims, NewClaim)
├── components/Sidebar.tsx
├── pages/
│   ├── DashboardPage.tsx
│   ├── ClaimsPage.tsx
│   └── NewClaimPage.tsx
├── api/claims.ts             fetch-обёртки: getClaims, getClaimById, getClaimLetter,
│                              transitionClaim, CreateClaim
├── App.css, index.css, main.tsx
```

Frontend пока **без auth-интеграции** — `fetch` без JWT-заголовка. Это первое что нужно подключить.

## Сервисы — кто за что

### `ClaimLifecycleServiceImpl` (lifecycle/)

Главный оркестратор. Любая операция над claim проходит через него.

Коллабораторы (8): `ClaimRepository`, `UserRepository`, `EligibilityService`, `ClaimWorkflowService`, `ClaimDocumentsService`, `EventsRepository`, `ClaimLetterService`, `ObjectMapper`, `ApplicationEventPublisher`.

**`NotificationService` НЕ инжектируется** — нотификации публикуются как domain events, listeners слушают.

Операции:
- `createClaim` → `resolveClaimOwner` (USER берёт свой email из SecurityContext, ADMIN может явно передать `userId`) → eligibility → save → `publishEvent(ClaimCreatedEvent)`.
- `updateClaimDetails` → `assertEditable` → partial update → recalc → save.
- `transition` → FSM validate → save claim+event → `publishEvent(ClaimStatusTransitionedEvent)`.
- Read: `getClaimById`, `getAllClaims`, `getClaimEvents`, `getClaimLetter` — каждый делает `assertOwnerOrAdmin(claim)`.

### `ClaimWorkflowServiceImpl`

FSM. Без БД, без I/O.

- `assertTransitionAllowed(from, to)` — `ALLOWED_TRANSITIONS` + `IllegalStateException` (→409).
- `autoPreSubmitStatus(current, hasAllDocs)` → DOCS_REQUESTED / READY_TO_SUBMIT.
- `assertEditable(status)` — запрещает редактирование после SUBMITTED.
- `eventType(target)` → EventTypes (Map dispatch).

**Только этот класс видит/меняет `ClaimStatus`.** `if (claim.getStatus()...)` где-то ещё — баг архитектуры.

### `EligibilityServiceImpl`

Pure rule engine. Делегирует в `EligibilityStrategy` по `IssueType`. Конструктор инжектит `List<EligibilityStrategy>`, строит `Map<IssueType, EligibilityStrategy>`.

| Стратегия | Тип | База |
|-----------|-----|------|
| `DelayEligibilityStrategy` | DELAY | EU 261, ≥180 мин, distance table 250/400/600 |
| `CancellationEligibilityStrategy` | CANCELLATION | EU 261, notice ≤14 дней |
| `MissedConnectionEligibilityStrategy` | MISSED_CONNECTION | EU 261, итоговая ≥180 мин |
| `BaggageDelayedEligibilityStrategy` | BAGGAGE_DELAYED | Montreal Art.19, ≥6ч, per-day €50 cap €500 |
| `BaggageLostEligibilityStrategy` | BAGGAGE_LOST | Montreal Art.17, >504ч (21 день), €1000 flat |
| `BaggageDamagedEligibilityStrategy` | BAGGAGE_DAMAGED | Montreal Art.17§2, ≤7 дней с доставки |

Добавление нового типа = новый `@Component implements EligibilityStrategy`. Сервис не трогается.

### `ClaimLetterServiceImpl`

Та же стратегийная архитектура. `LetterStrategy` per `IssueType`, шесть реализаций. Каждая отдаёт полный `LetterResponse{subject, body}` — тела для EU 261 и Montreal принципиально разные.

Validation precondition'ов (claim/user/flight/issue не null) — в самом сервисе, до делегирования.

### `EmailNotificationService`

Реализует `NotificationService` (методы `sendClaimCreated`, `sendClaimSubmitted`) И слушает события через `@TransactionalEventListener(AFTER_COMMIT)`.

- `onClaimCreated(ClaimCreatedEvent)` → `sendClaimCreated`.
- `onClaimTransitioned(ClaimStatusTransitionedEvent)` → диспетчер `Map<ClaimStatus, Consumer<Claim>>` вызывает нужный sender по `event.to()`.

`AFTER_COMMIT` гарантирует: если транзакция откатилась — email не уйдёт. Email failure ловится в catch внутри `send(...)`, основной flow не страдает.

### `FollowUpSchedulerService`

`@Scheduled(cron = "0 0 9 * * *")`. Раз в день:
1. `EventsRepository.findClaimIdsEligibleForFollowUp(threshold)` — claims в SUBMITTED >14 дней без ответа.
2. Ставит синтетический `ROLE_ADMIN` в `SecurityContextHolder` (cron бежит без HTTP-запроса).
3. Для каждого claim вызывает `claimService.transition(id, FOLLOW_UP_SENT)`.
4. Чистит `SecurityContextHolder` в `finally`.

### `DocumentStorageServiceImpl`

Файловое хранение + MIME-валидация по магическим байтам.
- Path traversal защита через `getSafePath`.
- Лимит 5MB.
- Allowlist: `application/pdf`, `image/jpeg`, `image/png`.
- Ownership-проверка в `uploadDocument`, `downloadDocument`/`getDocument`, `deleteDocument` — non-admin может только свои документы.

### `AuthServiceImpl`

Регистрация: создание User с `passwordHash` (BCrypt) + дефолтная `Role.USER` → выдача JWT с `email` (sub) + `role` (claim).

Login: проверка пароля → JWT с актуальной ролью из БД.

## Security в деталях

### Production (`app.security.enabled=true` по умолчанию)

`SecurityConfig` — stateless JWT chain:
- Public: `/api/auth/**`, `/`, `/app.html`, `/css/**`, `/js/**`.
- Всё остальное `authenticated()`.
- `@EnableMethodSecurity` — `@PreAuthorize` работает.

`JwtAuthFilter` встроен `addFilterBefore(UsernamePasswordAuthenticationFilter)`, читает `Authorization: Bearer <token>`, валидирует, ставит `JwtAuthentication` в SecurityContext.

### `@PreAuthorize` распределение

| Endpoint | Доступ |
|----------|--------|
| `POST /api/claims` | `isAuthenticated()` (USER создаёт свой, ADMIN может через `userId`) |
| `GET /api/claims/{id}` | `isAuthenticated()` + ownership |
| `GET /api/claims` (list) | `hasRole('ADMIN')` |
| `PATCH /api/claims/{id}/update` | `hasAnyRole('MODERATOR','ADMIN')` |
| `POST /api/claims/{id}/transition` | `hasAnyRole('MODERATOR','ADMIN')` |
| `GET /api/claims/{id}/events`/`letter` | `isAuthenticated()` + ownership |
| `POST/GET/DELETE /api/documents/**` | `isAuthenticated()` + ownership; delete — only `MODERATOR/ADMIN` |
| `/api/admin/users/**` | `hasRole('ADMIN')` (class-level) |
| `/api/scheduler/**` | `hasAnyRole('ADMIN')` (class-level, profile=dev) |

### Ownership

`assertOwnerOrAdmin(claim)` в lifecycle и `DocumentStorageServiceImpl`:
- `auth == null || !isAuthenticated()` → `AccessDeniedException`.
- `ROLE_ADMIN` → пропуск.
- `claim.getUser().getEmail() != auth.getName()` → `AccessDeniedException`.

**MODERATOR здесь не учитывается** — открытый вопрос продуктовой логики (нужно ли модератору видеть claims клиента которого ведёт).

### Tests (`app.security.enabled=false` через `IntegrationTestBase`)

- Production `SecurityConfig` НЕ загружается (`@ConditionalOnProperty(matchIfMissing=true)`).
- `TestSecurityConfig` (`havingValue="false"`) загружается — permit-all chain + custom filter ставит синтетический `ROLE_ADMIN` user в SecurityContext + provides `PasswordEncoder` bean.
- В `ClaimServiceImplTest` (Mockito unit) `@BeforeEach` руками выставляет `ROLE_ADMIN` auth, в `@AfterEach` чистит.

## Флоу запросов

### createClaim
```
POST /api/claims  +JWT
  → JwtAuthFilter ставит SecurityContext
  → @PreAuthorize("isAuthenticated()") пускает
  → @Transactional начинает
    → resolveClaimOwner(userId) — ADMIN: findById, USER: findByEmail из ctx
    → MapStruct map flight/issue/euContext
    → mapForCreate(documents)
    → recalcDerivedFields → eligibility + auto-status
    → claimRepository.save(claim)
    → eventPublisher.publishEvent(ClaimCreatedEvent)  [pending]
  → COMMIT
  → AFTER_COMMIT: EmailNotificationService.onClaimCreated → SMTP
  → response
```

### transition
```
POST /api/claims/{id}/transition  +JWT
  → @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
  → @Transactional
    → findWithDetailsById (EntityGraph)
    → assertTransitionAllowed
    → save claim + audit event
    → publishEvent(ClaimStatusTransitionedEvent)
  → COMMIT
  → AFTER_COMMIT: dispatcher по target → sendClaimSubmitted/etc
  → response
```

### Scheduled follow-up
```
cron 9:00 → checkForFollowUps
  → findClaimIdsEligibleForFollowUp(now-14d)
  → SecurityContextHolder.setAuthentication(systemAdmin)
  try {
    for each id: claimService.transition(id, FOLLOW_UP_SENT)
      [та же цепочка, что выше; email уходит после commit]
  } finally { SecurityContextHolder.clearContext() }
```

## Архитектурные правила

1. **`ClaimLifecycleServiceImpl`** — единственный orchestrator. Контроллер не вызывает другие сервисы напрямую.
2. **`ClaimWorkflowServiceImpl`** — единственное место где видят/меняют `ClaimStatus`.
3. **`EligibilityServiceImpl`** — pure, без I/O.
4. **Все стратегии — `@Component`**, добавление типа = новая стратегия. Сервисы не трогаются.
5. **Migrations через Flyway**. Никаких `ddl-auto=create/update` в проде.
6. **Notifications — через events**, lifecycle не знает об email/SMS.
7. **Ownership — в сервисе** (lifecycle/storage), не в контроллере; `@PreAuthorize` для role-check, ownership — рантайм-проверка.
8. **Все scheduled-методы** должны выставлять synthetic `SecurityContext` перед вызовом `@PreAuthorize`-защищённых методов.

## Известные ограничения

- `BoardingDocuments.deletedAt` — поле есть, soft-delete не реализован.
- `ClaimEvents.payload` — `TEXT`, не `jsonb`. Перед аналитикой мигрировать.
- `Claim.@OneToOne` (Flight/Issue/EuContext) фактически EAGER несмотря на `fetch=LAZY` (Hibernate ограничение для non-owning side, нужен bytecode enhancement).
- MODERATOR в `assertOwnerOrAdmin` не учтён — открытый вопрос продукта.
- Frontend без auth-интеграции (нет JWT-заголовка в `fetch`).
- Inbound email parsing нет.
- Локальное хранилище файлов — не S3.
- SDR/EUR конверсия захардкожена.
