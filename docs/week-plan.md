# Sprint plan — claims-mvp

Состояние на 2026-05-10.

История прошлых спринтов — в `docs/daily/`. Полный план развития — в `docs/roadmap.md`. Текущее состояние кода — в `docs/architecture-overview.md`.

---

## Закрыто к 2026-05-10

**Week 1 (2026-04-29 → 2026-05-03):**
- Стратегийная архитектура для `EligibilityServiceImpl` и `ClaimLetterServiceImpl`.
- `MISSED_CONNECTION`, `BAGGAGE_DELAYED` end-to-end.
- Email-инфраструктура (`Spring Mail`, MailHog dev, SMTP env prod).
- Event-driven notifications через `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`.
- Map-диспетчер transition-нотификаций.
- Home page + frontend-прототип (Thymeleaf + статический `app.html`).

**Week 2 (2026-05-04 → 2026-05-10):**
- `BAGGAGE_LOST` end-to-end (Montreal Art.17, >504ч, €1000 flat).
- `BAGGAGE_DAMAGED` end-to-end (Montreal Art.17§2, ≤7 дней с доставки) + миграция V3.
- Follow-up scheduler (`FollowUpSchedulerService`, cron 9:00 ежедневно).
- Spring Security + JWT + 3 роли (USER/MODERATOR/ADMIN).
- `@PreAuthorize` на всех endpoint'ах.
- Ownership-проверка в lifecycle и storage слоях.
- Auth endpoints (register/login).
- Admin endpoints (`GET /api/admin/users`, `PATCH /api/admin/users/{id}/role`).
- Миграции V4-V6 (password, role, seed admin).
- Frontend перевели на React 19 + Vite + TypeScript + React Router (`/frontend/`). Скелет: Sidebar, Dashboard, Claims, NewClaim, API-клиент.
- Множество silent bugs пойманы и закрыты:
  - Забытые `@Component` на стратегиях.
  - Инвертированная логика `assertOwnerOrAdmin`.
  - `createClaim` с `userId` в request body (фикс: брать из SecurityContext).
  - Конфликт двух `SecurityFilterChain` бинов в тестах (фикс: `@ConditionalOnProperty`).
  - Scheduler без security-контекста (фикс: synthetic `ROLE_ADMIN`).
  - `PasswordEncoder` бин исчезал в тестах.

---

## ✅ Закрыто — Спринт 1 (2026-05-19): «Закрыть value loop»

Главный gap продукта закрыт: письмо-претензия уходит в авиакомпанию при SUBMITTED/FOLLOW_UP_SENT. Frontend auth + ClaimDetailPage готовы.

### Day 1-2 — Outbound email в airline

При переходе claim в SUBMITTED:
- Считать получателя по `Flight.airline` (карта airline → email; для MVP — `customer@<airline-domain>` или таблица `airline_contact`).
- Генерировать `LetterResponse` через существующий `ClaimLetterService`.
- Отправить через `JavaMailSender` (новый метод в `EmailNotificationService` или отдельный `AirlineNotificationService`).
- Записать `EventTypes.EMAIL_SENT` в `ClaimEvents` с payload `{to, subject}`.

Acceptance:
- Интеграционный тест: создать claim → submit → verify `JavaMailSender.send()` вызван с `to` соответствующим airline-email.
- В `ClaimEvents` появляется запись `EMAIL_SENT`.

### Day 3-4 — Frontend auth integration

- `LoginPage`, `RegisterPage` под `/login`, `/register`.
- `apiClient` обёртка над `fetch` с автоматической подстановкой `Authorization: Bearer <token>` из `localStorage`.
- 401-handler: clear token + redirect на `/login`.
- Logout button в Sidebar.
- Убрать pages из защищённого роутера для неавторизованного пользователя.

Acceptance:
- Незалогиненный заходит → редирект на `/login`.
- Зашёл → токен в `localStorage` → редирект на `/`.
- Все API-вызовы из `api/claims.ts` идут с JWT.
- При истечении токена 401 ловится и редиректит обратно.

### Day 5 — Claim detail page на фронте

- Роут `/claims/:id` → `ClaimDetailPage`.
- Header: статус, eligible, compensation, claim id.
- Tab/section: события (timeline по `ClaimEvents`).
- Tab/section: letter (читаем `getClaimLetter`, выводим subject + body).
- Кнопки переходов FSM (только для MODERATOR/ADMIN — проверять role из decoded JWT).

Acceptance:
- Открыл `/claims/42` → вся инфа на странице.
- USER видит только свой claim, чужой → 403 (handler как 401).

---

## Текущий sprint (Спринт 2: «Automation polish + inbound»)

### Day 1-2 — ESCALATED логика
- После отсутствия ответа на follow-up → автоматический переход в `ESCALATED`.
- Scheduler: проверять claims в статусе `FOLLOW_UP_SENT` старше N дней → transition + email.

### Day 3 — ShedLock
- Подключить `ShedLock` чтобы scheduler не дублировался в multi-instance окружении.

### Day 4-5 — Inbound email parsing
- IMAP listener или webhook от провайдера.
- Rule-based классификация ответов airline (заглушка перед AI слоем).

---

## Следующие спринты (черновик)

**Sprint 3 — AI layer (Phase 4):**
- LLM-объяснения eligibility пользователю.
- Adaptive letter generation (заменить статичные шаблоны).
- Inbound classification через LLM.

**Sprint 4 — Polish:**
- S3 migration для storage.
- `ClaimEvents.payload` `TEXT → jsonb`.
- Analytics dashboard (Phase 6).
- 152-ФЗ compliance (если нужен российский рынок).

---

## Дисциплина

- В конце каждого «дня плана» — snapshot в `docs/daily/<date>.md`.
- После snapshot'а — обновить **этот файл** (отметить done) и `CLAUDE.md` (`Latest day` указатель).
- `agent-handoff.md` обновляется только когда меняется фундаментальный контекст (стек, правила, главные ловушки).
