# Чеклист по ТЗ — AI-агент по компенсациям от авиакомпаний

Состояние на 2026-04-29. Сверка оригинального ТЗ против текущей реализации `claims-mvp`.

Легенда: `[x]` — реализовано, `[~]` — частично, `[ ]` — не реализовано.

---

## MVP

- [x] форма создания кейса (`POST /api/claims`)
- [~] выбор типа проблемы (`IssueType`)
  - [x] задержка рейса (`DELAY`)
  - [x] отмена рейса (`CANCELLATION`)
  - [ ] missed connection
  - [ ] потеря багажа
  - [ ] задержка багажа
  - [ ] повреждение багажа
- [x] ввод данных рейса и бронирования (`Flight`, `EuContext`)
- [~] загрузка подтверждающих документов (`DocumentTypes`, `DocumentStorageService`)
  - [x] билет (`TICKET`)
  - [x] boarding pass (`BOARDING_PASS`)
  - [x] baggage tag (`BAG_TAG`)
  - [x] фото багажа (`PHOTO`)
  - [ ] переписка
- [x] базовая проверка права на компенсацию (`EligibilityService`)
- [x] генерация претензии / обращения (`ClaimLetterService`, EU 261)
- [~] отправка пользователю готового письма (`GET /api/claims/{id}/letter`)
  - [x] возврат текста письма
  - [ ] авто-отправка (нет email-интеграции)
- [x] сохранение кейса в CRM (`Claim` + `User`)
- [ ] базовые follow-up напоминания (есть статус `FOLLOW_UP_SENT`, но автоматики нет)
- [~] аналитика по статусам кейсов
  - [x] события и статусы хранятся (`ClaimEvents`)
  - [ ] дашборд / агрегации

---

## 1. Intake / Claim intake layer

- [x] создание кейса пользователем (`ClaimController.createClaim`)
- [x] сбор данных рейса
  - [x] номер рейса (`Flight.flightNumber`)
  - [x] дата (`Flight.flightDate`)
  - [x] маршрут (`Flight.routeFrom`/`routeTo`)
  - [x] авиакомпания (`Flight.airline`)
  - [x] booking reference (`Flight.bookingRef`)
  - [x] причина обращения (`Issue.type`)
- [~] загрузка файлов (`DocumentController.uploadDocument`)
  - [x] билет / маршрутная квитанция
  - [x] посадочный талон
  - [x] багажная квитанция
  - [ ] PIR report
  - [x] фото повреждений (общая категория `PHOTO`)
  - [ ] чеки на расходы
- [x] определение категории кейса (`IssueType`, `EligibilityService.isFlightClaim`)

---

## 2. Eligibility / Rules engine

- [x] rule-based проверка права на компенсацию (`EligibilityServiceImpl` — pure function, no side effects)
- [~] логика по сценариям
  - [x] delay compensation (≥180 мин, EU 261)
  - [x] cancellation compensation (≤14 дней до вылета)
  - [ ] denied boarding
  - [ ] missed connection
  - [ ] delayed baggage
  - [ ] lost baggage
  - [ ] damaged baggage
- [~] учёт
  - [x] страны / юрисдикции (`EuContext.departureFromEu`/`euCarrier`)
  - [x] длительности задержки (`Issue.delayMinutes`)
  - [x] дистанции перелёта (`Flight.distanceKm`, табличный расчёт 250/400/600 EUR)
  - [x] carrier type (EU / non-EU)
  - [x] исключения (extraordinary circumstances)
- [x] определение
  - [x] есть ли право на claim (`EligibilityResult.eligible`)
  - [x] какой тип claim подходит (по `IssueType`)
  - [x] какие документы ещё нужны (`EligibilityResult.requiredDocuments`)

---

## 3. AI-assistant layer

- [ ] объяснение пользователю
  - [ ] есть ли шанс на компенсацию (есть только boolean флаг, нет нарратива)
  - [ ] на чём основано решение
  - [ ] какие действия нужны дальше
- [~] генерация
  - [x] претензии в авиакомпанию (`ClaimLetterService` — EU 261, шаблон)
  - [ ] follow-up писем (LLM)
  - [ ] escalation шаблонов
  - [ ] писем в страховую / посредника
- [~] адаптация текста под форматы
  - [x] email (текстовое тело письма)
  - [ ] web form
  - [ ] PDF letter

> Layer не покрыт: нет интеграции с LLM, нет AI-объяснений и контекстной адаптации.

---

## 4. Claim workflow / Automation

- [x] FSM воронки кейса (`ClaimWorkflowService`, `ClaimStatus`)
  - [x] new → docs requested → ready to submit → submitted → follow-up sent → approved / rejected → paid → closed
  - [ ] escalated (нет статуса)
- [x] валидация переходов (`assertTransitionAllowed`, 409 на запрещённых)
- [x] объединённый эндпоинт `POST /api/claims/{id}/transition`
- [ ] автоматические follow-up действия
  - [ ] напоминание пользователю по таймеру
  - [ ] повторное письмо авиакомпании
  - [ ] escalation через заданный интервал
- [ ] шаблоны последовательных касаний
- [x] лог истории всех действий (`ClaimEvents` + типизированные `EventTypes`)

---

## 5. CRM-блок

- [x] карточка пользователя (`User`, `UserController`)
- [x] карточка claim case (`Claim`, `ClaimController.getClaimById`)
- [~] история кейсов по пользователю (`User.claims` есть в модели, но нет endpoint'а GET для всех claims user'а)
- [x] статусы (`ClaimStatus` — open/pending/won/lost/paid покрыты)
- [ ] сегментация
  - [ ] по авиакомпании
  - [ ] по типу претензии
  - [ ] по стране / юрисдикции
  - [ ] по сумме компенсации
- [ ] lead capture для незавершённых кейсов

---

## 6. Communications layer

- [ ] email-уведомления
- [ ] Telegram / WhatsApp / push
- [ ] полуавтоматическая или автоматическая отправка претензий

> Слой полностью отсутствует. Только генерация текста письма (GET /letter) — без отправки.

---

## 7. Content / SEO layer

- [~] базовая web-страница (`HomeController` + Thymeleaf)
- [ ] SEO-страницы под типовые кейсы
- [ ] образовательный контент
- [ ] автогенерация посадочных страниц

---

## 8. Integrations layer

- [ ] email providers
- [~] CRM (есть свой минимальный CRM-модуль внутри проекта)
- [ ] e-sign / document generation
- [~] storage для документов (локальная FS, не S3/Cloudinary)
- [ ] travel data lookup / flight status APIs
- [ ] legal / case-management tools

---

## 9. Analytics

- [ ] количество созданных кейсов (на уровне БД доступно, дашборда нет)
- [ ] доля успешно доведённых кейсов
- [ ] средняя сумма компенсации
- [ ] конверсия по этапам воронки
- [ ] аналитика по авиакомпаниям / маршрутам / типам
- [ ] funnel drop-off

---

## Особые требования

- [x] основная логика eligibility — rule-based (`EligibilityServiceImpl`, не AI)
- [x] AI не даёт юридических обещаний (AI-слоя пока нет вообще)
- [~] логирование
  - [x] входных данных (через JPA persistence)
  - [x] принятое решение (`Claim.eligible`, `compensationAmount`)
  - [ ] отправленные письма (нет факта отправки)
  - [x] изменения статусов (`ClaimEvents` с типизированными event-types)
- [x] upload и безопасное хранение документов
  - [x] валидация по магическим байтам (не доверяем Content-Type клиента)
  - [x] path traversal protection (`getSafePath`)
  - [x] лимит размера (5 MB), allowlist MIME
- [ ] учёт дедлайнов подачи claims и напоминания о них
- [~] полуавтоматический режим
  - [x] AI готовит все документы (текст письма генерится)
  - [ ] пользователь подтверждает отправку (нет интеграции с email)
- [~] reusable claims engine
  - [x] `EligibilityService` — pure rule engine, легко расширяется
  - [x] `ClaimWorkflowService` — FSM с EnumMap, легко добавлять статусы
  - [ ] не протестировано на расширение в train/hotel/insurance

---

## Стек по ТЗ vs факт

| Слой | По ТЗ | Факт |
|------|-------|------|
| Backend | Python FastAPI или NestJS | Java 21 + Spring Boot 4 |
| DB | PostgreSQL | PostgreSQL + Flyway |
| Frontend | Next.js | Thymeleaf (заглушка) |
| AI / LLM | LLM-интеграция | нет |
| Rules engine | отдельный модуль | `EligibilityServiceImpl` |
| Storage | S3 / Cloudinary | локальная FS |
| OCR | optional | нет |
| Queue | Redis + BullMQ / Celery | нет |
| Analytics | PostHog / dashboard | нет |
| Admin | claims dashboard | нет |

> Стек на бэкенде расходится с ТЗ (Java вместо Python/Node). Ядро бизнес-логики — eligibility, FSM, CRM, документы — реализовано, periphery (LLM, очереди, нотификации, аналитика, фронт) — нет.

---

## Сводка

**Готово к работе:**
- Ядро eligibility и compensation (EU 261/2004 для DELAY и CANCELLATION).
- Полный FSM жизненного цикла claim'а с аудит-логом.
- Загрузка/хранение/выдача документов с защитой от подмены MIME и path traversal.
- Базовый CRM (user + claim + events).
- Генерация претензионного письма.
- Унифицированный REST API: создание, обновление, переход по статусу, скачивание документов, получение писем.

**Большие пробелы относительно ТЗ:**
1. Покрытие типов проблем — 2 из 6 (только DELAY и CANCELLATION; нет багажных кейсов и missed connection).
2. AI-слой полностью отсутствует — нет LLM, нет объяснений пользователю, нет адаптивной генерации follow-up'ов.
3. Communications layer пуст — нет email, Telegram, WhatsApp, нет автоматической отправки претензий.
4. Automation — есть только статусы, нет таймеров, нет автоматических напоминаний и escalation.
5. Analytics — данные есть в БД, но нет дашборда и агрегатов.
6. Frontend — заглушка на Thymeleaf вместо Next.js.
7. Integrations — нет flight status API, нет S3, нет очередей.

**Качество того что реализовано:**
- Архитектура чистая: orchestrator (`ClaimLifecycleService`) + изолированные модули (`Workflow`, `Eligibility`, `Documents`, `Storage`, `Letter`).
- Безопасность по документам закрыта (магические байты, path traversal, orphan-cleanup).
- HTTP-семантика корректная (400/404/409/500 разнесены по типам ошибок).
- JPA-перформанс: LAZY + EntityGraph + пагинация.
- Покрытие тестами: unit (Mockito) + integration (TestContainers) — все зелёные.
