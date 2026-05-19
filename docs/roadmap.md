# Roadmap — claims automation platform

Состояние на 2026-05-10. План развития от текущего состояния (MVP+ с покрытием всех кейсов и базовой автоматизацией) до полной реализации видения ТЗ.

---

## Текущая точка

Реализовано:
- Все 6 типов кейсов (DELAY, CANCELLATION, MISSED_CONNECTION, BAGGAGE_DELAYED, BAGGAGE_LOST, BAGGAGE_DAMAGED) end-to-end через стратегийную архитектуру.
- FSM жизненного цикла claim (10 статусов, включая ESCALATED) + аудит-лог через `ClaimEvents`.
- Eligibility и letter — оба pure rule-based, делегируют в стратегии. Добавление нового типа = один `@Component`-класс.
- Email-инфраструктура: `JavaMailSender` + `@TransactionalEventListener(AFTER_COMMIT)` — eventual consistency.
- Notifications через domain events (`ClaimCreatedEvent`, `ClaimStatusTransitionedEvent`).
- Follow-up scheduler: cron 9:00 ежедневно, авто-перевод SUBMITTED → FOLLOW_UP_SENT после 14 дней.
- Spring Security + JWT + 3 роли (USER/MODERATOR/ADMIN), `@EnableMethodSecurity`, `@PreAuthorize` на всех endpoint'ах.
- Ownership-проверка: USER видит/редактирует только свои claim'ы и документы. ADMIN — всё.
- Admin-управление: `GET /api/admin/users`, `PATCH /api/admin/users/{id}/role`.
- Auth-эндпоинты: register / login.
- Загрузка/хранение документов с защитой от MIME-spoofing и path traversal, ownership на download/delete.
- Frontend: React 19 + Vite + TypeScript SPA, скелет с Sidebar/Dashboard/Claims/NewClaim, API-клиент.
- Покрытие тестами: 78+ (unit Mockito + integration TestContainers + WebMvcTest).
- 6 миграций Flyway.

Прогноз: ~50% от полного видения ТЗ закрыто.

---

## Прогресс по фазам

### Phase 1 — Communications layer

- ✅ Email-провайдер (Spring Mail, MailHog dev / SMTP prod).
- ✅ Email-уведомления при createClaim и при переходе в SUBMITTED.
- ✅ Event-driven с AFTER_COMMIT.
- ❌ Auto-send претензии в **авиакомпанию** (сейчас отправляется только пользователю, не airline). См. Phase 1.1 ниже.
- ❌ Inbound email parsing (приём ответов авиакомпаний).
- ❌ Telegram / WhatsApp нотификации.
- ❌ Шаблоны для остальных событий: docs requested, won, paid, closed, escalated.

### Phase 2 — Coverage breadth

- ✅ Все 6 типов кейсов.
- ❌ DENIED_BOARDING (ТЗ упоминал, но в скоуп не вошёл).
- ❌ DocumentTypes.RECEIPTS (баг: тесты упоминают, в enum нет).

### Phase 3 — Automation / Follow-up scheduler

- ✅ Базовый scheduler: SUBMITTED → FOLLOW_UP_SENT после 14 дней (фиксированный порог).
- ✅ Статус ESCALATED заведён в FSM.
- ❌ Escalation paths: после Y дней без ответа на follow-up → ESCALATED. Сейчас в код не заложено.
- ❌ Deadline tracking: каждая юрисдикция имеет свой срок подачи (UK 6 лет, DE 3 года, ...). Отдельная таблица + cron.
- ❌ Напоминания пользователю о неполных документах.
- ❌ Idempotency защита при перезапуске scheduler'а.
- ❌ Multi-instance safety (нужен `ShedLock`).

### Phase 4 — AI / LLM layer ❌ Не начата

Ничего не реализовано.

### Phase 5 — Frontend

- ✅ Скелет SPA: React 19 + Vite + React Router.
- ✅ Pages: Dashboard, Claims list, NewClaim form.
- ✅ API-клиент: getClaims/getClaimById/getClaimLetter/transitionClaim/CreateClaim.
- ❌ Auth integration (login/register UI, JWT в headers, 401-handling).
- ❌ Claim detail page (просмотр, событий, letter).
- ❌ Document upload UI с прогрессом.
- ❌ Admin-панель (управление пользователями, список всех claims).
- ❌ Mobile responsive, design polish.
- ❌ Real-time / progress feedback при долгих операциях.

### Phase 6+ — не начаты

Phase 6 (Analytics/Admin), Phase 7 (SEO), Phase 8 (Scale), Phase 9 (Generic engine) — пока не актуальны.

---

## Что делать дальше — приоритезация

Ключевая дыра: **продукт не «отправляет в авиакомпанию» — только генерирует текст и шлёт пользователю**. Это разрыв value loop'а.

### Спринт 1 — Закрыть петлю «реально отправляет претензию»

**Phase 1.1 — Outbound email в авиакомпанию** (~1-2 дня):
- При переходе в SUBMITTED отправлять `LetterResponse.body` на `customer@<airline>.com` (или конфигурируемый адрес).
- Тестовые airline-mailbox'ы хардкодом или через таблицу `airline_contact`.
- Логировать факт отправки (`EventTypes.EMAIL_SENT`).

**Frontend auth integration** (~1-2 дня):
- Login/Register pages.
- JWT хранение (`localStorage` для MVP, потом httpOnly cookie).
- API-клиент: `Authorization: Bearer <token>`.
- 401 → редирект на login.
- Logout.

**Claim detail page на фронте** (~1 день):
- Просмотр одного claim'а: header, статус, события, letter.
- Кнопки переходов по FSM (для MODERATOR/ADMIN).

### Спринт 2 — Дотюнить automation

**Phase 3 продолжение** (~3-5 дней):
- Escalation: FOLLOW_UP_SENT → ESCALATED через N дней без ответа.
- Deadline tracking: таблица юрисдикций, cron-проверка дедлайнов, нотификация пользователю.
- Напоминания пользователю о недозагруженных документах.
- `ShedLock` для multi-instance.

**Inbound email parsing** (~3-5 дней):
- IMAP-listener или webhook от провайдера (SendGrid Inbound Parse).
- Парсинг ответа → классификация (заглушка перед AI: rules + keywords).
- Запись `ClaimEvents.AIRLINE_RESPONSE`.
- Запуск escalation logic если negative.

### Спринт 3 — AI / LLM (Phase 4)

**AI explanations** (~1 неделя):
- Интеграция с OpenAI/Anthropic API.
- Объяснения eligibility пользователю (почему да/нет, на чём основано).
- Подсказки о документах.

**Adaptive letter generation** (~1-2 недели):
- Замена статичных шаблонов в `LetterStrategy` на LLM-генерацию.
- Сохранение rule-based как fallback.
- Контекст кейса в промпте.

**Inbound classification** (~1 неделя):
- Замена rule-based парсинга на LLM-классификацию ответов авиакомпании.
- Генерация контр-аргументов / escalation писем.

### Спринт 4+ — Polish и операционная готовность

- Перенести storage на S3 (Yandex Object Storage / Cloudinary).
- Migrate `ClaimEvents.payload` с `TEXT` на `jsonb` для аналитики.
- Bytecode enhancement plugin для true LAZY на `@OneToOne`.
- Soft-delete на `BoardingDocuments`.
- 152-ФЗ compliance (если рынок РФ): регистрация оператора ПДн, аудит-лог обращений к ПДн, эндпоинты прав субъекта (выгрузка/удаление).
- Analytics dashboard (Phase 6).
- SEO landing pages (Phase 7).

---

## Рекомендация на ближайший спринт

**Брать Спринт 1.** Конкретно — **outbound email в авиакомпанию** первым: это закрывает основной value-prop продукта. После него frontend auth + claim detail page — продукт можно показывать пользователю.

После спринта 1 продукт реально работает: пассажир регистрируется → создаёт claim → автоматически уходит письмо в авиакомпанию → scheduler сам делает follow-up.

Спринт 2 (inbound + escalation) и Спринт 3 (AI) — после.

## Критические тех-долги

Не блокируют функционал, но накапливаются:
- Frontend без error handling и loading states.
- Нет CI (`.github/workflows/`).
- Hardcoded SMTP в test profile (если SMTP лежит — log error, но тесты могут шуметь).
- Отсутствие централизованной обработки 401/403 на фронте.
