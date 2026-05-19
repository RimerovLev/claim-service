# Чеклист по ТЗ — claims-mvp

Состояние на 2026-05-10. Сверка оригинального ТЗ против текущей реализации.

Легенда: `[x]` — реализовано, `[~]` — частично, `[ ]` — не реализовано.

---

## MVP

- [x] форма создания кейса (`POST /api/claims`)
- [x] выбор типа проблемы — все 6 типов:
  - [x] задержка рейса (`DELAY`)
  - [x] отмена рейса (`CANCELLATION`)
  - [x] missed connection (`MISSED_CONNECTION`)
  - [x] потеря багажа (`BAGGAGE_LOST`)
  - [x] задержка багажа (`BAGGAGE_DELAYED`)
  - [x] повреждение багажа (`BAGGAGE_DAMAGED`)
- [x] ввод данных рейса и бронирования (`Flight`, `EuContext`, `Issue`)
- [~] загрузка подтверждающих документов
  - [x] билет (`TICKET`)
  - [x] boarding pass (`BOARDING_PASS`)
  - [x] baggage tag (`BAG_TAG`)
  - [x] PIR report (`PIR`)
  - [x] фото (`PHOTO`)
  - [ ] переписка как тип документа
  - [ ] чеки (`RECEIPTS` — в тестах упомянут, в enum нет)
- [x] базовая проверка права на компенсацию (`EligibilityServiceImpl` + 6 стратегий)
- [x] генерация претензии / обращения (`ClaimLetterServiceImpl` + 6 стратегий)
- [~] отправка пользователю готового письма
  - [x] email пользователю при createClaim
  - [x] email пользователю при transition в SUBMITTED
  - [ ] **отправка письма в авиакомпанию** (критическая дыра — продукт пока не «доходит» до airline)
- [x] сохранение кейса в CRM (`Claim` + `User` + `ClaimEvents`)
- [~] базовые follow-up напоминания
  - [x] cron 9:00 ежедневно: SUBMITTED → FOLLOW_UP_SENT после 14 дней
  - [ ] напоминания пользователю о недозагруженных документах
  - [ ] эскалация после неответа на follow-up
- [~] аналитика по статусам кейсов
  - [x] события и статусы в БД (`ClaimEvents`, типизированные `EventTypes`)
  - [ ] дашборд / агрегаты
  - [ ] funnel-метрики

---

## 1. Intake / Claim intake layer

- [x] создание кейса пользователем
- [x] полный набор данных рейса (number, date, route, airline, bookingRef)
- [x] категоризация (`IssueType`)
- [~] загрузка файлов
  - [x] основные типы (TICKET, BOARDING_PASS, BAG_TAG, PIR, PHOTO)
  - [x] валидация по магическим байтам, allowlist MIME, лимит 5MB
  - [x] path traversal protection
  - [ ] чеки на расходы (`RECEIPTS`)

---

## 2. Eligibility / Rules engine

- [x] rule-based, не AI-based
- [x] логика по сценариям — все 6 типов:
  - [x] delay compensation (EU 261)
  - [x] cancellation compensation (EU 261)
  - [x] missed connection (EU 261)
  - [x] delayed baggage (Montreal Art.19)
  - [x] lost baggage (Montreal Art.17)
  - [x] damaged baggage (Montreal Art.17§2)
  - [ ] denied boarding (есть в EU 261, не реализован)
- [x] учёт юрисдикции (EU vs Montreal), длительности, дистанции, carrier type, extraordinary
- [x] определение eligible / суммы / required documents
- [x] стратегийная архитектура — добавить новый тип = один класс

---

## 3. AI-assistant layer ❌ Не начата

- [ ] объяснение пользователю (почему есть/нет права, на чём основано, что дальше)
- [ ] LLM-генерация претензий (сейчас статические шаблоны)
- [ ] follow-up / escalation шаблоны через LLM
- [ ] адаптация под email/web form/PDF
- [ ] guardrails (rule-based проверка перед LLM-выходом)

---

## 4. Claim workflow / Automation

- [x] FSM воронки (10 статусов, включая ESCALATED)
- [x] валидация переходов (`assertTransitionAllowed`, 409)
- [x] унифицированный transition endpoint
- [x] аудит-лог всех действий (`ClaimEvents` + `EventTypes`)
- [~] автоматический follow-up
  - [x] базовый cron 9:00, переход submitted → follow_up_sent после 14д
  - [ ] переход follow_up_sent → escalated
  - [ ] tunable временные окна (сейчас захардкожено 14)
  - [ ] idempotency / multi-instance (нет `ShedLock`)
- [ ] templates последовательных касаний (нет конфигурации шаблонов)
- [ ] deadline tracking по юрисдикциям

---

## 5. CRM-блок

- [x] карточка пользователя (`User`)
- [x] карточка claim case (`Claim`)
- [~] история кейсов по пользователю
  - [x] `User.claims` в модели
  - [ ] endpoint типа `GET /api/users/{id}/claims` для своих кейсов
- [x] статусы (FSM полная)
- [ ] сегментация (по airline, типу, юрисдикции, сумме)
- [ ] lead capture для незавершённых кейсов

---

## 6. Communications layer

- [~] email-уведомления
  - [x] инфраструктура (Spring Mail + events)
  - [x] claim created
  - [x] claim submitted
  - [ ] **отправка в авиакомпанию** (только пользователю)
  - [ ] docs requested
  - [ ] won / paid / closed / rejected / escalated
- [ ] Telegram / WhatsApp / push
- [ ] inbound email parsing (приём ответов авиакомпании)
- [~] полуавтоматическая / автоматическая отправка
  - [x] AI-letter готов автоматически
  - [ ] auto-send в airline при SUBMITTED

---

## 7. Content / SEO layer

- [~] базовая страница: HomeController + Thymeleaf на `/`
- [ ] SEO-страницы под типы кейсов
- [ ] образовательный контент
- [ ] автогенерация landing pages

---

## 8. Integrations layer

- [ ] email providers — есть Spring Mail SMTP (MailHog dev), нет SendGrid/Postmark/SES
- [~] CRM — собственный, минимальный
- [ ] e-sign / document generation
- [~] storage — локальная FS, не S3/Cloudinary
- [ ] travel data lookup / flight status APIs
- [ ] legal / case-management tools

---

## 9. Analytics

- [ ] количество кейсов / процент выигрыша / средние суммы
- [ ] конверсия по этапам воронки
- [ ] аналитика по airline / маршрутам / типам
- [ ] funnel drop-off

Данные есть в БД (`ClaimEvents`), интерфейсов аналитики нет.

---

## Особые требования

- [x] основная логика eligibility — rule-based
- [x] AI не даёт юридических обещаний (AI-слой пока не подключён вообще)
- [x] логирование
  - [x] входных данных (JPA-persistence)
  - [x] принятые решения (`Claim.eligible/compensationAmount`)
  - [x] изменения статусов (`ClaimEvents`)
  - [ ] факт отправки писем в авиакомпанию (нет outbound)
- [x] upload и безопасное хранение документов
  - [x] валидация по магическим байтам
  - [x] path traversal protection
  - [x] лимит размера (5MB), allowlist MIME
  - [ ] шифрование at-rest
- [ ] учёт дедлайнов подачи claims и напоминания
- [~] полуавтоматический режим
  - [x] AI готовит документ
  - [ ] пользователь подтверждает отправку (нет UI этого flow)
- [x] reusable claims engine
  - [x] `EligibilityService` — rule engine, легко расширяемый
  - [x] `ClaimWorkflowService` — FSM с EnumMap
  - [ ] не валидирован на расширение в train/hotel/insurance

---

## Безопасность (соответствие)

- [x] Spring Security + JWT (stateless)
- [x] BCrypt для паролей
- [x] 3 роли (USER, MODERATOR, ADMIN)
- [x] `@PreAuthorize` на всех endpoint'ах
- [x] Ownership-проверка в сервисном слое
- [ ] 2FA / TOTP
- [ ] Anomaly detection (geo/device fingerprint)
- [ ] Rate limiting / anti-bot
- [ ] CAPTCHA на формах
- [ ] Регистрация оператора ПДн в РКН (152-ФЗ)
- [ ] Шифрование PII в БД
- [ ] Эндпоинты прав субъекта (выгрузка/удаление по 152-ФЗ)
- [ ] Резервный SMS-провайдер
- [ ] Audit log обращений к ПДн (не только к claims)

---

## Стек: ТЗ vs факт

| Слой | По ТЗ | Факт |
|------|-------|------|
| Backend | Python FastAPI / NestJS | **Java 21 + Spring Boot 4** |
| DB | PostgreSQL | PostgreSQL + Flyway |
| Frontend | Next.js | **React 19 + Vite + TS** (SPA) |
| AI / LLM | LLM-интеграция | нет |
| Rules engine | отдельный модуль | `EligibilityServiceImpl` (стратегии) |
| Storage | S3 / Cloudinary | локальная FS |
| OCR | optional | нет |
| Queue | Redis + BullMQ / Celery | нет (sync + cron) |
| Analytics | PostHog / dashboard | нет |
| Admin | claims dashboard | минимальный API (`/api/admin/users`) |
| Auth | (не описано) | JWT + 3 роли |

---

## Сводная оценка

**Backend ядро:** ~80% от MVP-объёма ТЗ.

**Communications:** ~40%. Главный gap — outbound email в airline.

**Automation:** ~30%. Базовый cron есть, escalation/deadlines нет.

**AI-слой:** 0%. Самая «продающая» часть продукта не начата.

**Frontend:** ~25%. Скелет есть, нет auth и detail-страниц.

**Compliance/Security:** ~50%. Authentication/authorization готовы. 152-ФЗ, 2FA, anti-bot — отсутствуют.

**В целом:** ~50% от полного видения ТЗ. Backend крепкий, frontend и AI — впереди работа.
