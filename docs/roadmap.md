# Roadmap — claims automation platform

Состояние на 2026-04-29. План развития от текущего MVP-каркаса до видения из ТЗ.

Базовая логика: каждая фаза даёт самостоятельную ценность и снимает зависимость для следующей. Зависимости и возможности параллелизации указаны явно.

---

## Текущая точка (Phase 0 — DONE)

Реализовано на бэкенде:
- Ядро eligibility (EU 261/2004) для DELAY и CANCELLATION.
- FSM жизненного цикла claim с аудит-логом (`ClaimEvents`).
- Загрузка и хранение документов с защитой от MIME-spoofing и path traversal.
- Базовый CRM: user → claim → events → documents.
- Генерация претензионного письма (текст).
- Унифицированный REST API: create / update / transition / документы / письмо.
- Покрытие тестами: unit (Mockito) + integration (TestContainers).

Осталось до полного видения ТЗ — порядка 80% объёма. Дальше — по фазам.

---

## Прогресс (обновлено 2026-05-06)

### Phase 1 — Communications layer 🔄 В процессе

Сделано:
- ✅ `spring-boot-starter-mail` подключён, SMTP-конфиг (MailHog dev / env vars prod).
- ✅ `NotificationService` интерфейс + `EmailNotificationService` реализация.
- ✅ Нотификация пользователю при создании claim (`ClaimCreatedEvent`).
- ✅ Нотификация пользователю при переходе в SUBMITTED (`ClaimStatusTransitionedEvent`).
- ✅ Event-based архитектура: `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` — eventual consistency гарантирована.
- ✅ Map-диспетчер для transition-нотификаций — добавление нового статуса = одна строка в Map.

Не сделано (Week 2):
- ❌ Auto-send претензии в авиакомпанию при SUBMITTED (Day 3 Week 2).
- ❌ Inbound email parsing.
- ❌ Telegram / WhatsApp нотификации.

### Phase 2 — Coverage breadth 🔄 4/6 типов

- ✅ `DELAY` — EU 261/2004, distance-based compensation.
- ✅ `CANCELLATION` — EU 261/2004, notice ≤ 14 дней.
- ✅ `MISSED_CONNECTION` — EU 261/2004, итоговое опоздание ≥ 3ч.
- ✅ `BAGGAGE_DELAYED` — Montreal Convention Art. 19, per-day allowance.
- ❌ `BAGGAGE_LOST` — Montreal Convention Art. 17 (Day 1 Week 2).
- ❌ `BAGGAGE_DAMAGED` — Montreal Convention Art. 17 §2 (Day 2 Week 2).

Архитектурный тех-долг из Phase 2 (был в roadmap) — **закрыт**:
- ✅ `EligibilityServiceImpl` разбит на стратегии (`Map<IssueType, EligibilityStrategy>`).
- ✅ `ClaimLetterServiceImpl` разбит на стратегии (`Map<IssueType, LetterStrategy>`).

### Phase 3 — Automation / Follow-up scheduler ❌ Не начата

Планируется Day 4 Week 2 (базовый follow-up cron).

### Phase 4 — AI / LLM layer ❌ Не начата

### Phase 5 — Frontend ❌ Не начата (есть статический прототип)

Сделано вне фазы:
- ✅ Thymeleaf home page (`/`) — описание продукта, 4 типа кейсов, status check.
- ✅ Статический прототип `app.html` — Dashboard + Claims list + New claim form + Detail panel.
- Подключение к реальному API — Day 5 Week 2.

---

## Phase 1 — Communications layer

**Цель.** Замкнуть петлю «AI готовит письмо → оно реально уходит в авиакомпанию» и пользователь получает уведомления о ходе кейса. Без этого продукт остаётся «генератором текста», а не агентом.

**Скоуп:**
- Email-провайдер (SendGrid / Postmark / AWS SES) — модуль `notifications/email`.
- Шаблоны email-уведомлений: claim created, docs requested, letter sent, follow-up due, won, paid, closed.
- Автоматическая отправка претензии в авиакомпанию (через email-бэкенд) при переходе в `SUBMITTED`.
- Inbound email parsing (опционально, для следующих фаз) — приём ответов от авиакомпаний.
- Опционально: Telegram / WhatsApp нотификации для пользователя.

**Зависимости.** Нет — можно стартовать прямо сейчас.

**Оценка.** 4-6 недель для одного разработчика.

**Ключевые решения:**
- Шаблоны: статические Thymeleaf или генерация через LLM (см. Phase 4).
- Доставка: синхронно или через очередь (Redis / RabbitMQ) — выбор зависит от Phase 3.

---

## Phase 2 — Coverage breadth (новые типы кейсов)

**Цель.** Расширить продукт с 2 до 6 типов проблем — из ТЗ. Это удваивает (минимум) addressable market.

**Скоуп:**
- `IssueType.MISSED_CONNECTION` + правила eligibility.
- `IssueType.BAGGAGE_DELAYED` + правила (Montreal Convention).
- `IssueType.BAGGAGE_LOST` + правила.
- `IssueType.BAGGAGE_DAMAGED` + правила.
- `IssueType.DENIED_BOARDING` (есть в EU 261).
- Новые `DocumentTypes`: PIR (есть), receipts, baggage photos с метаданными.
- Шаблоны писем под каждый тип кейса (`ClaimLetterService` → стратегия с подклассами по `IssueType`).
- Расширение `EligibilityServiceImpl` под новые сценарии — он уже rule-based, добавление новых веток дешёвое.

**Зависимости.** Нет от Phase 1 — можно параллелить.

**Оценка.** 4-6 недель. Каждый тип кейса — это правила + шаблон письма + (минорно) валидация документов.

**Архитектурные риски:**
- `EligibilityServiceImpl.evaluate` сейчас однометодный — для 6 типов лучше разбить на стратегию: `Map<IssueType, EligibilityStrategy>`.
- `ClaimLetterServiceImpl` тоже монолитен — аналогично.

---

## Phase 3 — Automation / Follow-up scheduler

**Цель.** Превратить FSM из «руками двигаемого» в реально автоматизированный агент. Это вторая ключевая часть ТЗ — «агент дожимает кейс».

**Скоуп:**
- Job scheduler (Quartz или Spring `@Scheduled` + `ShedLock` для multi-instance).
- Правила follow-up: «через X дней после `SUBMITTED` без ответа → автоматический follow-up».
- Escalation paths: «через Y дней без ответа на follow-up → escalation level 2».
- Новый статус `ESCALATED` (есть в ТЗ, нет в коде).
- Deadline tracking: каждая юрисдикция имеет срок подачи (EU — 6 лет в UK, 3 в Германии, etc.) — отдельная таблица + cron-проверка.
- Напоминания пользователю если он не дозагрузил документы.

**Зависимости.** Phase 1 (нужно отправлять письма).

**Оценка.** 3-4 недели.

**Ключевые решения:**
- Где хранить расписание: в БД (`scheduled_actions` таблица) или в job scheduler. Рекомендую первое — проще аудитить.
- Idempotency: если scheduler перезапустится, follow-up не должен уйти дважды.

---

## Phase 4 — AI / LLM layer

**Цель.** Добавить главную фишку из названия проекта — «AI-агент». Сейчас это просто rule-based pipeline.

**Скоуп:**
- Интеграция с LLM-провайдером (OpenAI / Anthropic).
- Персонализированные explanations для пользователя:
  - почему есть/нет права на компенсацию;
  - какие документы и почему нужны;
  - что делать дальше.
- Адаптивная генерация претензий: вместо статичного шаблона — учёт специфики кейса (длинная задержка, ребёнок на борту, потерянная связь, и т.д.).
- Анализ ответов авиакомпании (если есть inbound email из Phase 1):
  - классификация: согласие / отказ / запрос документов / эскалация;
  - генерация контр-аргумента или escalation письма.
- Guardrails: AI не делает юридических утверждений, всегда есть rule-based проверка (требование ТЗ).

**Зависимости.** Phase 1 (для отправки и приёма писем). Полезно после Phase 3 (накопится данных по follow-up'ам).

**Оценка.** 4-6 недель для core, плюс непрерывное улучшение.

**Ключевые решения:**
- Промпт-стратегия: один универсальный промпт на тип задачи или fine-tuned модели.
- Кэширование / контекст: для одного кейса генерируется много текстов — нужен общий контекст в LLM-state.
- Контроль качества: human-in-the-loop на старте (полу-автомат из ТЗ), полная автоматизация позже.

---

## Phase 5 — Frontend (Next.js)

**Цель.** Сейчас бэкенд используется только через API — пользователь не сможет работать с продуктом сам. Без фронта продукта в продакте нет.

**Скоуп:**
- Next.js приложение, ssr-friendly.
- Onboarding flow: создание кейса, прогрессивное раскрытие полей.
- Drag-drop загрузка документов с превью.
- Дашборд статусов кейсов пользователя.
- Просмотр / редактирование сгенерированного письма.
- Уведомления и история событий.
- Mobile-responsive.

**Зависимости.** Можно стартовать с самого начала параллельно (если есть отдельная команда). Но для полноценного релиза нужны Phase 1 (нотификации) и Phase 4 (AI explanations).

**Оценка.** 4-8 недель. Зависит от уровня дизайна и количества flow.

---

## Phase 6 — Analytics + Admin panel

**Цель.** Дать команде ops/product/legal видимость в работу системы. Без этого нельзя итерироваться.

**Скоуп:**
- Funnel-метрики: started → submitted → won → paid (и где отваливаются).
- Сегментация: по авиакомпании, типу кейса, юрисдикции, сумме.
- Cohort retention.
- Самые проблемные airlines / маршруты.
- Admin-панель для case manager'а:
  - просмотр кейса со всей историей;
  - ручное вмешательство (приостановить scheduler, изменить статус с обходом FSM, добавить заметку);
  - повторная отправка письма;
  - bulk operations.
- Интеграция с PostHog / Mixpanel / собственный dashboard на Grafana.

**Зависимости.** Phase 1-3 — нужны живые данные. Phase 5 (для общего стиля UI).

**Оценка.** 3-4 недели для базовой версии.

---

## Phase 7 — SEO / Content

**Цель.** Привлечение трафика. Это growth-фаза, не product.

**Скоуп:**
- Landing pages по типам кейсов: «compensation for delayed flight», «lost baggage claim», и т.д.
- Программная генерация SEO-страниц: airline × city, route × delay reason.
- Образовательный блог: права пассажира, EU 261 vs Montreal, как собрать доказательства.
- Lead capture для пользователей, начавших кейс но не дозагрузивших документы.
- Структурированные данные / FAQ-схемы.

**Зависимости.** Phase 5 (фронтенд должен быть). Phase 6 для funnel-аналитики.

**Оценка.** Continuous, base launch — 4-6 недель.

---

## Phase 8 — Integrations / Scale

**Цель.** Перевести проект из «работает на одной машине» в «работает у тысяч пользователей».

**Скоуп:**
- Storage: S3 / Cloudinary вместо локальной FS (текущая реализация в `DocumentStorageServiceImpl` готова к замене — есть абстракция `getSafePath`).
- Flight status API: FlightAware / AviationStack — для верификации задержек, полёт реально был, и т.д.
- Очередь задач: Redis Streams / RabbitMQ / Kafka для async-операций.
- OCR для фото и PDF — извлечение полей из boarding pass / receipts. Опционально, заметно улучшает UX.
- Multi-region storage / GDPR compliance.

**Зависимости.** Phase 1-3 должны работать. Можно начинать сразу при росте нагрузки.

**Оценка.** По модулям, 1-3 недели на каждый.

---

## Phase 9 — Reusable claims engine

**Цель.** Превратить продукт из «travel claims» в «claims automation platform» — расширение из ТЗ.

**Скоуп:**
- Generalize ядро: `Claim`, `EligibilityEngine`, `Workflow`, `Documents` как модели предметной области, не зашитые под travel.
- Domain-specific overlays:
  - train compensation (Eurostar, DB, etc.).
  - hotel disputes.
  - insurance claims.
  - travel refunds.
- Pluggable rule engines per domain — каждый со своим `EligibilityStrategy`.
- White-label для travel-агентств: B2B API + branding.
- Multi-tenancy.

**Зависимости.** Всё предыдущее. Это long-term направление, на горизонте 12+ месяцев.

**Оценка.** Большое — 3-6 месяцев на полноценный generic-engine.

---

## Сводная схема зависимостей

```
Phase 0 (MVP) ─┬─> Phase 1 (Comms) ─┬─> Phase 3 (Automation) ─┐
               │                     │                          │
               ├─> Phase 2 (Coverage)│                          ├─> Phase 6 (Analytics) ─> Phase 7 (SEO)
               │                     └─> Phase 4 (AI) ──────────┤
               └─> Phase 5 (Frontend, можно параллельно) ────────┘
                                                                  │
                                                                  └─> Phase 8 (Scale) ─> Phase 9 (Generic engine)
```

---

## Рекомендуемые порядки

### Если один разработчик / маленькая команда
1. **Phase 1** — communications. Это разблокирует ценностное предложение «агент реально что-то делает».
2. **Phase 2** — coverage. Минимум missed connection и delayed baggage — это самые частые случаи у пользователей.
3. **Phase 3** — automation. Теперь продукт реально автоматический.
4. **Phase 5** — frontend. Без него нельзя продавать.
5. **Phase 4** — AI. Полировка персонализации.
6. **Phase 6** — analytics.
7. **Phase 7** — SEO.

Дальше Phase 8/9 по необходимости.

### Если есть отдельная фронт-команда
- Параллелить **Phase 5 (frontend)** с **Phase 1-2 (backend basics)** с самого начала.
- Бэкенд: 1 → 2 → 3 → 4.
- Фронт: skeleton → onboarding → dashboard → AI chat → admin.

### Если фокус на demo/инвесторов
- **Phase 5 + минимальный Phase 4** в первую очередь — нужно красиво продемонстрировать AI-агента.
- Phase 1 с одним email-провайдером.
- Coverage расширим позже.

---

## Технический долг и предусловия

Перед стартом Phase 2 имеет смысл:
- Разбить `EligibilityServiceImpl.evaluate` на стратегии (`Map<IssueType, EligibilityStrategy>`).
- Аналогично `ClaimLetterServiceImpl`.
- Это не блокер, но добавление 4 новых типов в монолитный метод будет грязно.

Перед Phase 3:
- Перенести storage на абстракцию (готово, есть `DocumentStorageService` интерфейс) — но сама реализация на локальной FS, перед prod надо сразу на S3.
- Migrations / Flyway: сейчас `@Column(name = ...)` управляет именами; перед production — review всех миграций.

Перед Phase 8:
- `Claim.events` `@OneToMany` без пагинации — для long-running кейсов с сотнями событий это станет узким местом.
- `events.payload` хранится как `TEXT`, а не `jsonb` — для аналитики Phase 6 удобнее `jsonb`.

---

## Приоритет №1 — что делать прямо сейчас

С точки зрения «MVP → реальный работающий продукт» правильный следующий шаг — **Phase 1 (communications)**. Без него весь FSM, follow-up статусы и аудит — мёртвый код, потому что письма никуда не уходят. Это разблокирует Phase 3, и продукт становится реальным агентом.

Параллельно можно начинать Phase 2 (coverage) — она независимая и расширяет market reach.

Phase 4 (AI) — самая «продающая» часть, но без Phase 1 и 3 ей нет в чём проявиться.
