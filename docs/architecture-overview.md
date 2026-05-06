# Архитектура claims-mvp — что и где

Состояние на 2026-05-06.

## Что делает приложение в одном абзаце

Приложение принимает заявки от пассажиров на компенсацию за задержанные / отменённые рейсы и потери/повреждения багажа под EU 261/2004 и Montreal Convention 1999. Пользователь создаёт claim с данными рейса и инцидента, прикладывает документы, система автоматически считает право на компенсацию и сумму, генерирует претензионное письмо, отправляет email-уведомления и ведёт claim через FSM-воронку (NEW → READY_TO_SUBMIT → SUBMITTED → APPROVED/REJECTED → PAID → CLOSED) с аудит-логом каждого шага.

---

## Данные — какие объекты живут в БД

```
User
 └── Claim (один user → много claims)
      ├── Flight (1:1)         — номер рейса, авиакомпания, маршрут, дата, дистанция
      ├── Issue (1:1)          — тип инцидента + тип-специфичные поля:
      │                            delayMinutes (DELAY, MISSED_CONNECTION)
      │                            cancellationNoticeDays (CANCELLATION)
      │                            baggageDelayHours (BAGGAGE_DELAYED, BAGGAGE_LOST)
      │                            daysSinceDelivery (BAGGAGE_DAMAGED) — V3 миграция
      │                            extraordinary (все типы)
      ├── EuContext (1:1)      — euCarrier? departureFromEu?
      ├── BoardingDocuments[]  — документы: TICKET, BOARDING_PASS, PIR, BAG_TAG, PHOTO, RECEIPTS
      └── ClaimEvents[]        — аудит-лог: тип события + JSON payload
```

`Claim` хранит **derived поля** — `eligible`, `compensationAmount`, `status`. Пересчитываются при каждом create/update через `recalcDerivedFields`.

**Миграции Flyway:**
- V1 — базовая схема.
- V2 — `baggage_delay_hours` в `issues`.
- V3 — `days_since_delivery` в `issues` (BAGGAGE_DAMAGED, Week 2 Day 2).

---

## Пакеты — что где живёт

```
com.claims.mvp/
├── claim/
│   ├── controller/         — ClaimController, DocumentController
│   ├── service/
│   │   ├── lifecycle/      — ClaimLifecycleServiceImpl (оркестратор)
│   │   ├── workflow/       — FSM: переходы + event type lookup
│   │   ├── documents/      — ClaimDocumentsServiceImpl (merge/map документов)
│   │   ├── storage/        — DocumentStorageServiceImpl (диск + MIME-валидация)
│   │   └── letter/
│   │       ├── ClaimLetterServiceImpl  (делегатор)
│   │       └── strategy/   — LetterStrategy + DelayLetterStrategy,
│   │                          CancellationLetterStrategy, MissedConnectionLetterStrategy,
│   │                          BaggageDelayedLetterStrategy  [+ Lost, Damaged — Week 2]
│   ├── dao/                — ClaimRepository, BoardingDocumentsRepository
│   ├── dto/request|response/
│   ├── mapper/             — MapStruct: ClaimMapper, ClaimEntityMapper, DocumentMapper
│   ├── model/              — Claim, Flight, Issue, EuContext, BoardingDocuments
│   └── enums/              — ClaimStatus, IssueType, DocumentTypes, EventTypes
│
├── eligibility/
│   ├── service/            — EligibilityServiceImpl (делегатор)
│   ├── strategy/           — EligibilityStrategy + DelayEligibilityStrategy,
│   │                          CancellationEligibilityStrategy, MissedConnectionEligibilityStrategy,
│   │                          BaggageDelayedEligibilityStrategy  [+ Lost, Damaged — Week 2]
│   └── dto/response/       — EligibilityResult
│
├── notifications/
│   ├── NotificationService              — интерфейс (sendClaimCreated, sendClaimSubmitted)
│   ├── EmailNotificationService         — реализация: JavaMailSender + @TransactionalEventListener
│   └── events/
│       ├── ClaimCreatedEvent            — record(Claim claim)
│       └── ClaimStatusTransitionedEvent — record(Claim claim, ClaimStatus from, ClaimStatus to)
│
├── events/                 — ClaimEvents аудит-лог
│   ├── dao/                — EventsRepository
│   ├── model/              — ClaimEvents
│   └── dto/response/       — EventsResponse
│
├── user/
│   ├── controller/         — UserController
│   ├── service/            — UserServiceImpl
│   ├── dao/                — UserRepository
│   ├── model/              — User
│   ├── mapper/             — UserMapper
│   └── dto/                — CreateUserRequest, UserResponse
│
├── exception/              — GlobalExceptionHandler, DuplicateUserException
└── web/                    — HomeController (Thymeleaf /)
```

**Статика:**
- `src/main/resources/templates/home.html` — Thymeleaf главная страница.
- `src/main/resources/static/app.html` — standalone frontend-прототип (пока хардкод, Week 2 Day 5 — реальный API).

---

## Сервисы — кто за что отвечает

### `ClaimLifecycleServiceImpl` (lifecycle/)

Главный оркестратор. Любая операция над claim проходит через него.

Коллабораторы (8 зависимостей):
`ClaimRepository`, `UserRepository`, `EligibilityService`, `ClaimWorkflowService`, `ClaimDocumentsService`, `EventsRepository`, `ClaimLetterService`, `ObjectMapper`, `ApplicationEventPublisher`.

> **Важно:** `NotificationService` больше **не инжектируется** в Lifecycle. Нотификации публикуются через `ApplicationEventPublisher` как domain events. Это развязывает транзакцию и side-effect.

Операции:
- `createClaim` → eligibility check → save → `publishEvent(ClaimCreatedEvent)`.
- `updateClaimDetails` → partial update → recalc derived fields → save.
- `transition` → FSM validate → save → audit event → `publishEvent(ClaimStatusTransitionedEvent)`.
- Read: `getClaimById`, `getAllClaims`, `getClaimEvents`, `getClaimLetter`.

### `ClaimWorkflowServiceImpl` (workflow/)

Все правила про статусы и переходы. Чистый — без БД, без side-effects.

- `assertTransitionAllowed(from, to)` — валидация по `ALLOWED_TRANSITIONS`. 409 если нельзя.
- `autoPreSubmitStatus(current, hasAllDocs)` → DOCS_REQUESTED или READY_TO_SUBMIT.
- `assertEditable(status)` — запрещает редактирование после SUBMITTED.
- `eventType(target)` → EventTypes.

**Ничто за пределами этого класса не должно проверять или менять `ClaimStatus`.**

### `EligibilityServiceImpl` (eligibility/service/)

**Pure rule engine** — никакого I/O, только функция от входных данных.

Принимает `(Issue, Flight, EuContext, List<BoardingDocuments>)` → `EligibilityResult { eligible, compensationAmount, requiredDocuments }`.

**Архитектура стратегий** (с Week 1):
- Конструктор инжектирует `List<EligibilityStrategy>`, строит `Map<IssueType, EligibilityStrategy>`.
- `evaluate()` — делегирует в стратегию по `issue.getType()`. Никакой бизнес-логики в самом сервисе.
- Добавление нового типа = новый `@Component` класс, реализующий `EligibilityStrategy`. Сервис не трогается.

Текущие стратегии:
| Стратегия | Тип | Правовая база |
|-----------|-----|---------------|
| `DelayEligibilityStrategy` | DELAY | EU 261, ≥180 мин, distance table |
| `CancellationEligibilityStrategy` | CANCELLATION | EU 261, notice ≤14 дней |
| `MissedConnectionEligibilityStrategy` | MISSED_CONNECTION | EU 261, итог ≥180 мин |
| `BaggageDelayedEligibilityStrategy` | BAGGAGE_DELAYED | Montreal Art.19, ≥6ч, per-day €50 |
| `BaggageLostEligibilityStrategy` | BAGGAGE_LOST | Montreal Art.17, >504ч (21 дней) — Week 2 |
| `BaggageDamagedEligibilityStrategy` | BAGGAGE_DAMAGED | Montreal Art.17§2, ≤7 дней с доставки — Week 2 |

### `ClaimLetterServiceImpl` (letter/)

Генерирует текст претензионного письма. **Та же стратегийная архитектура** что у Eligibility.

- Конструктор инжектирует `List<LetterStrategy>` → `Map<IssueType, LetterStrategy>`.
- Валидация preconditions (claim/user/flight/issue не null) остаётся в сервисе — общая для всех стратегий.
- Каждая стратегия отвечает за полный `LetterResponse { subject, body }` — намеренно, тела для EU 261 и Montreal принципиально разные.

### `EmailNotificationService` (notifications/)

Отправляет письма в реакции на domain events. Реализует `NotificationService`.

**Event listeners** (`@TransactionalEventListener(AFTER_COMMIT)`):
- `onClaimCreated(ClaimCreatedEvent)` → `sendClaimCreated` — письмо пользователю.
- `onClaimTransitioned(ClaimStatusTransitionedEvent)` → диспетчер по `event.to()`.

**Map-диспетчер** `transitionHandlers: Map<ClaimStatus, Consumer<Claim>>`:
- `SUBMITTED` → `sendClaimSubmitted` (пользователю) + `sendClaimLetterToAirline` (авиакомпании, Week 2 Day 3).
- Добавление новой нотификации = одна строка в Map.

Конструктор принимает `(JavaMailSender mailSender, @Value("${app.mail.from}") String from)`.

> **Паттерн AFTER_COMMIT:** нотификация гарантированно фаерится только после успешного коммита транзакции. Если транзакция откатилась — письмо не уйдёт.

### `ClaimDocumentsServiceImpl` (documents/)

Маппинг и merge документов. Чистый — без I/O.
- `mapForCreate` — DTO → entities.
- `mergeForUpdate` — merge по типу (один тип = одна запись).
- `uploadedTypes` → Set\<DocumentTypes\>.

### `DocumentStorageServiceImpl` (storage/)

Физическое хранение файлов на диске.
- Magic bytes валидация, лимит 5MB, allowlist MIME.
- Path traversal защита через `getSafePath`.

---

## Поток: создание claim

```
POST /api/claims
    │
    ▼
ClaimLifecycleService.createClaim
    ├─> UserRepository.findById
    ├─> ClaimEntityMapper (flight, euContext, issue → entities)
    ├─> ClaimDocumentsService.mapForCreate
    │
    ├─> recalcDerivedFields:
    │     ├─> EligibilityService.evaluate
    │     │     └─> Map<IssueType, EligibilityStrategy>.get(type).evaluate()
    │     ├─> ClaimDocumentsService.uploadedTypes
    │     └─> ClaimWorkflowService.autoPreSubmitStatus
    │
    ├─> ClaimRepository.save
    ├─> ApplicationEventPublisher.publishEvent(ClaimCreatedEvent)
    │     └─> [AFTER_COMMIT] EmailNotificationService.onClaimCreated
    │               └─> mailSender.send (пользователю)
    └─> ClaimMapper.toResponse
```

## Поток: переход статуса

```
POST /api/claims/{id}/transition
    │
    ▼
ClaimLifecycleService.transition
    ├─> ClaimRepository.findWithDetailsById
    ├─> ClaimWorkflowService.assertTransitionAllowed   [409 если нельзя]
    ├─> ClaimWorkflowService.eventType(target)
    ├─> buildTransitionPayload (JSON)
    ├─> claim.setStatus(target)
    ├─> ClaimRepository.save
    ├─> EventsRepository.save                          [аудит]
    ├─> ApplicationEventPublisher.publishEvent(ClaimStatusTransitionedEvent)
    │     └─> [AFTER_COMMIT] EmailNotificationService.onClaimTransitioned
    │               └─> transitionHandlers.get(to)?.accept(claim)
    └─> ClaimMapper.toResponse
```

## Поток: добавление нового типа кейса

```
Шаг 1: IssueType.java — добавить enum-значение
Шаг 2: EligibilityStrategy — новый @Component класс
Шаг 3: LetterStrategy — новый @Component класс
Шаг 4: Тесты (юнит + интеграционный)
Шаг 5: ClaimServiceImplTest.setUp() — добавить стратегии в List.of(...)

Сервисы EligibilityServiceImpl и ClaimLetterServiceImpl — не трогать.
Spring подхватит @Component автоматически через List<Strategy> инъекцию.
```

---

## Где какие правила живут

| Правило | Где менять |
|---------|------------|
| Разрешённые переходы статусов | `ClaimWorkflowServiceImpl.ALLOWED_TRANSITIONS` |
| Event-type при переходе | `ClaimWorkflowServiceImpl.EVENT_BY_TARGET` |
| Eligible / compensation / required docs | `*EligibilityStrategy` по типу кейса |
| Шаблон письма в авиакомпанию | `*LetterStrategy` по типу кейса |
| Нотификации по статусу | `EmailNotificationService.transitionHandlers` Map |
| Допустимые MIME при загрузке | `DocumentStorageServiceImpl.ALLOWED_MIME_TYPES` |
| HTTP-коды для исключений | `GlobalExceptionHandler` |

---

## Тесты — структура

| Файл | Тип | Что тестирует |
|------|-----|---------------|
| `ClaimServiceImplTest` | Unit (MockitoExtension) | Lifecycle orchestration, event publication, FSM transitions |
| `EligibilityServiceImplTest` | Unit | Все стратегии eligibility по каждому типу кейса |
| `ClaimIntegrationTest` | Integration (TestContainers) | Full HTTP flow, статусы, события, письма, нотификации |
| `EmailNotificationServiceTest` | Unit | Listeners, sendClaimCreated, sendClaimSubmitted, error suppression |
| `ClaimControllerTest` | Unit (MockMvc) | HTTP-слой, валидация запросов |
| `UserControllerTest` | Unit (MockMvc) | User CRUD, duplicate email |

**Ключевые особенности:**
- Интеграционные тесты расширяют `IntegrationTestBase` (TestContainers PostgreSQL).
- `@MockitoBean JavaMailSender` в `ClaimIntegrationTest` — мокает транспорт, но оставляет `EmailNotificationService` живым вместе с его listeners.
- `ClaimServiceImplTest` вручную собирает `ClaimLifecycleServiceImpl` через конструктор со всеми стратегиями — Spring-контекст не поднимается.

---

## Принципы, которые держат архитектуру чистой

1. **Lifecycle — единственная точка входа.** Контроллер не вызывает Workflow / Eligibility / Letters напрямую.
2. **Workflow — единственное место, где видят `ClaimStatus`.** `if (claim.getStatus() == ...)` вне этого класса — баг архитектуры.
3. **Eligibility — без I/O.** Чистая функция, аргументами приходит всё что нужно.
4. **Стратегии — через `@Component` + `List<T>` инъекцию.** Добавление типа кейса не требует правок в сервисах.
5. **Нотификации — через domain events, AFTER_COMMIT.** Lifecycle не знает о каналах доставки.
6. **MapStruct для всех конвертаций.** Ручной маппинг только в тестах.
7. **Flyway only** для схемы. Никакого `ddl-auto=create/update`.

---

## Known issues (не трогать без задачи)

- `BoardingDocuments.deletedAt` — поле есть, soft-delete логики нет.
- `ClaimEvents.payload` хранится как `TEXT`, не `jsonb` — нужно перед аналитикой Phase 6.
- `Claim` `@OneToOne` с `LAZY` — де-факто EAGER из-за Hibernate ограничений на non-owning side. Требует bytecode enhancement для реального lazy.
- `app.html` — данные хардкодены, подключение к API запланировано на Week 2 Day 5.
