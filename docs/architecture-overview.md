# Архитектура claims-mvp — что и где

Состояние на 2026-04-29.

## Что делает приложение в одном абзаце

Приложение принимает заявки от пассажиров на компенсацию за задержанные / отменённые рейсы под EU 261/2004. Пользователь создаёт claim с данными рейса и инцидента, прикладывает документы (билет, boarding pass), система автоматически считает право на компенсацию и сумму, генерирует претензионное письмо в авиакомпанию, и ведёт claim через FSM-воронку (NEW → READY_TO_SUBMIT → SUBMITTED → APPROVED/REJECTED → PAID → CLOSED) с аудит-логом каждого шага.

---

## Данные — какие объекты живут в БД

```
User
 └── Claim (один user → много claims)
      ├── Flight (1:1)         — данные рейса: номер, авиакомпания, маршрут, дата, дистанция
      ├── Issue (1:1)          — что случилось: тип, минуты задержки, дни до отмены, extraordinary
      ├── EuContext (1:1)      — eu-карьер? вылет из EU?
      ├── BoardingDocuments[]  — приложенные документы (билет, boarding pass, PIR, photo)
      └── ClaimEvents[]        — аудит-лог: STATUS_CHANGED / LETTER_SUBMITTED / CLAIM_APPROVED / ...
```

Сам `Claim` хранит **derived поля** — `eligible` (boolean), `compensationAmount` (int EUR), `status` (enum). Эти поля пересчитываются при создании и обновлении claim, чтобы не считать заново на каждое чтение.

---

## Пакеты — что где живёт

```
com.claims.mvp/
├── claim/
│   ├── controller/         — REST endpoints (ClaimController, DocumentController)
│   ├── service/
│   │   ├── lifecycle/      — оркестратор всех операций над claim (ClaimLifecycleService)
│   │   ├── workflow/       — FSM: разрешённые переходы + event type lookup
│   │   ├── documents/      — мапинг и merge документов в claim
│   │   ├── storage/        — физическая загрузка/чтение файлов с диска
│   │   └── letter/         — генерация претензионного письма
│   ├── dao/                — JPA-репозитории (ClaimRepository, BoardingDocumentsRepository)
│   ├── dto/request|response/ — REST-контракты
│   ├── mapper/             — MapStruct: entity ↔ DTO
│   ├── model/              — JPA-сущности (Claim, Flight, Issue, EuContext, BoardingDocuments)
│   └── enums/              — ClaimStatus, IssueType, DocumentTypes, EventTypes
│
├── eligibility/            — правила компенсации (pure rule engine)
│   ├── service/            — EligibilityServiceImpl
│   └── dto/response/       — EligibilityResult
│
├── events/                 — аудит-лог
│   ├── dao/                — EventsRepository
│   ├── model/              — ClaimEvents
│   └── dto/response/       — EventsResponse
│
├── user/                   — пользователи
│   ├── controller/         — UserController
│   ├── service/            — UserServiceImpl
│   ├── dao/                — UserRepository
│   ├── model/              — User
│   ├── mapper/             — UserMapper
│   └── dto/                — CreateUserRequest, UserResponse
│
├── exception/              — GlobalExceptionHandler + DuplicateUserException
└── web/                    — HomeController (Thymeleaf-страница)
```

---

## Сервисы — кто за что отвечает

### `ClaimLifecycleServiceImpl` (lifecycle/)
Главный оркестратор. **Любая** операция над claim проходит через него.
- `createClaim` — создание с initial eligibility check.
- `updateClaimDetails` — частичный update полей (запрещён после SUBMITTED).
- `transition` — единственная точка для смены статуса (FSM).
- read-only лукапы (`getClaimById`, `getAllClaims`, `getClaimEvents`, `getClaimLetter`).

Сам ничего не решает — делегирует в специализированные сервисы. Знает 7 коллабораторов: Workflow, Eligibility, Documents, Letter, Repositories, Mappers, ObjectMapper.

### `ClaimWorkflowServiceImpl` (workflow/)
Все правила про **статусы и переходы**. Чистый — без БД, без сайд-эффектов.
- `assertTransitionAllowed(from, to)` — валидация по `ALLOWED_TRANSITIONS` (Map<from, Set<to>>). Если нельзя — `IllegalStateException` (→ 409).
- `autoPreSubmitStatus(current, hasAllRequiredDocs)` — определяет DOCS_REQUESTED / READY_TO_SUBMIT по наличию документов.
- `assertEditable(current)` — запрещает редактирование claim после SUBMITTED.
- `eventType(target)` — лукап семантического event-type по target-статусу (Map<target, EventType>).

**Ничто за пределами этого класса не должно проверять и менять `ClaimStatus`** — это правило в CLAUDE.md.

### `EligibilityServiceImpl` (eligibility/)
**Pure rule engine.** Никакого I/O, никаких БД — функция от входных данных.
- Принимает Issue, Flight, EuContext, List\<BoardingDocuments\>.
- Возвращает `EligibilityResult { eligible, compensationAmount, requiredDocuments }`.

Логика:
- in scope: вылет из EU **или** EU-перевозчик.
- delay eligible: тип DELAY + delayMinutes ≥ 180.
- cancellation eligible: тип CANCELLATION + notice ≤ 14 дней.
- extraordinary circumstances выключает компенсацию.
- Сумма по таблице расстояний: ≤1500km → 250€, ≤3500km → 400€, иначе 600€.

### `ClaimDocumentsServiceImpl` (documents/)
**Мапинг и merge** документов внутри claim — чисто доменная логика, без I/O.
- `mapForCreate(dtos, claim)` — конвертирует DTO в entities при создании claim.
- `mergeForUpdate(claim, dtos)` — мерж новых документов с существующими **по типу** (один тип = одна запись, новый URL заменяет старый).
- `uploadedTypes(claim)` — возвращает Set\<DocumentTypes\>, какие типы уже загружены.

### `DocumentStorageServiceImpl` (storage/)
**Физическое хранение файлов**. В отличие от `ClaimDocumentsService`, тут есть I/O (диск).
- `uploadDocument` — валидирует файл по магическим байтам, сохраняет на диск, создаёт запись в БД.
- `getDocument` / `downloadDocument` — выдача файла по UUID.
- `deleteDocument`.
- `validateFile(file)` — magic bytes check, лимит 5MB, allowlist MIME (PDF/JPEG/PNG).

Защита от path traversal через `getSafePath`.

### `ClaimLetterServiceImpl` (letter/)
Генерирует **текст претензионного письма** под EU 261 — без отправки.
- На вход — целый Claim.
- На выход — `LetterResponse { subject, body }`.
- Шаблон в коде через text block (`""" ... """`).

### `UserServiceImpl` (user/)
Минимум: только `createUser` с проверкой дубликата email + race-protection через `DataIntegrityViolationException`.

---

## Поток: создание claim

```
POST /api/claims  + CreateClaimRequest
    │
    ▼
ClaimController.createClaim
    │
    ▼
ClaimLifecycleService.createClaim
    ├─> UserRepository.findById                    [load passenger]
    ├─> ClaimEntityMapper.toEntity(flight)         [DTO → Flight entity]
    ├─> ClaimEntityMapper.toEntity(euContext)      [DTO → EuContext]
    ├─> ClaimEntityMapper.toEntity(issue)          [DTO → Issue]
    ├─> ClaimDocumentsService.mapForCreate         [docs → entities]
    │
    ├─> recalcDerivedFields:
    │     ├─> EligibilityService.evaluate          [→ eligible, compensation, requiredDocs]
    │     ├─> ClaimDocumentsService.uploadedTypes  [→ uploaded types]
    │     └─> ClaimWorkflowService.autoPreSubmitStatus  [→ DOCS_REQUESTED or READY_TO_SUBMIT]
    │
    ├─> ClaimRepository.save                       [persist all via cascade]
    └─> ClaimMapper.toResponse                     [entity → DTO]
```

## Поток: переход статуса

```
POST /api/claims/{id}/transition  + { status: APPROVED, note: "..." }
    │
    ▼
ClaimController.transition
    │
    ▼
ClaimLifecycleService.transition
    ├─> ClaimRepository.findWithDetailsById        [load claim with EntityGraph]
    ├─> ClaimWorkflowService.assertTransitionAllowed   [validate FSM]
    ├─> ClaimWorkflowService.eventType(APPROVED)        [→ CLAIM_APPROVED]
    ├─> buildTransitionPayload                          [JSON via Jackson]
    ├─> claim.setStatus(APPROVED)
    ├─> ClaimRepository.save
    └─> EventsRepository.save                       [audit entry]
```

## Поток: загрузка документа

```
POST /api/documents/upload  (multipart: file, claimId, type, description)
    │
    ▼
DocumentController.uploadDocument
    │
    ▼
DocumentStorageService.uploadDocument
    ├─> validateFile                                [magic bytes → detected MIME]
    ├─> ClaimRepository.findById                    [load claim]
    ├─> generateUniqueFileName                      [UUID + extension]
    ├─> create BoardingDocuments entity
    ├─> attach to claim
    ├─> file.transferTo(disk)                       [I/O — file on disk]
    └─> ClaimRepository.saveAndFlush                [DB row]
        ↑ если ошибка — Files.deleteIfExists для отката
```

---

## Где какие правила живут (важно для расширения)

| Правило | Где менять |
|---------|------------|
| Какие переходы статусов разрешены | `ClaimWorkflowServiceImpl.ALLOWED_TRANSITIONS` |
| Какой event-type писать при переходе | `ClaimWorkflowServiceImpl.EVENT_BY_TARGET` |
| Когда claim eligible / какая сумма / какие документы нужны | `EligibilityServiceImpl.evaluate` |
| Шаблон письма в авиакомпанию | `ClaimLetterServiceImpl.generateLetter` |
| Какие MIME можно загружать | `DocumentStorageServiceImpl.ALLOWED_MIME_TYPES` |
| Маппинг HTTP-кодов на исключения | `GlobalExceptionHandler` |

---

## Где смотреть что-то конкретное

**«Почему claim не получился eligible?»** — `EligibilityServiceImpl.evaluate`, плюс глянуть `Issue` и `EuContext` в БД.

**«Почему этот переход не разрешён?»** — `ClaimWorkflowServiceImpl.ALLOWED_TRANSITIONS`.

**«Почему документ не валиден / не сохранился?»** — `DocumentStorageServiceImpl.validateFile` + `uploadDocument`.

**«Что происходит при создании claim?»** — `ClaimLifecycleServiceImpl.createClaim` (короткая, читается за минуту).

**«Как добавить новый тип кейса?»** — нужно дополнить `IssueType`, расширить `EligibilityServiceImpl.evaluate` (или вынести в стратегии — это в плане Day 1-2 недели), добавить ветку в `ClaimLetterServiceImpl`.

**«Где лежит история claim?»** — `ClaimEvents` таблица, `EventsRepository.findByClaimIdOrderByCreatedAtDesc`. Каждый переход и важное действие пишет туда запись с типизированным `EventTypes` и JSON-payload'ом.

---

## Принципы, которые держат архитектуру чистой

1. **Lifecycle — единственная точка входа.** Ни один контроллер не вызывает Workflow / Eligibility / Documents напрямую — только через Lifecycle.
2. **Workflow — единственное место, где видят `ClaimStatus`.** Если где-то ещё появляется `if (claim.getStatus() == ...)`, это баг архитектуры.
3. **Eligibility — без I/O.** Никаких БД-вызовов, никаких side effects. Если нужны данные — приходят аргументами.
4. **MapStruct для всех конвертаций.** Никакого ручного `new Flight(); flight.setX(...)` в продовом коде (есть только в тестах).
5. **MIME проверяется по магическим байтам**, не по client Content-Type.
6. **Soft-delete не используется** (поле `deletedAt` в `BoardingDocuments` есть, но логики нет — это known issue в CLAUDE.md).
7. **JPA: LAZY + EntityGraph**. Read-методы помечены `@Transactional(readOnly = true)`, для load-with-details есть `findWithDetailsById`.
