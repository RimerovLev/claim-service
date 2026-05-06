# Plans — claims-mvp

---

# Week 1 — ✅ DONE

**Итог:** рефакторинг на стратегии (eligibility + letter), 4 типа кейсов, email-инфраструктура на Spring Events, тесты.

**Прогресс:**
- [x] Day 1 — Eligibility strategies (DELAY, CANCELLATION)
- [x] Day 2 — Letter strategies (DELAY, CANCELLATION)
- [x] Day 3 — Missed connection end-to-end
- [x] Day 4 — Baggage delayed (Montreal Convention)
- [x] Day 5 + post — Email starter + event refactor + тесты

Coverage: 4/6 claim types. Email-слой работает, нотификации при создании и submit уходят.

---

# Week 2 — Phase 2 завершение + Phase 1 продолжение + Phase 3 старт

**Цель недели:**
- Закрыть Phase 2 (6/6 типов кейсов).
- Сделать письмо реально уходящим в авиакомпанию (Phase 1 — главный gap).
- Запустить базовый follow-up scheduler (Phase 3 — первый шаг к автоматизации).
- Подключить фронт-прототип к реальному API.

**Прогресс:**
- [ ] Day 1 — BAGGAGE_LOST
- [ ] Day 2 — BAGGAGE_DAMAGED
- [ ] Day 3 — Auto-send letter to airline on SUBMIT
- [ ] Day 4 — Follow-up scheduler
- [ ] Day 5 — Frontend → real API

---

## Day 1 — BAGGAGE_LOST (Phase 2)

**Цель.** Добавить шестой тип кейса — потеря багажа под Montreal Convention. Архитектура готова: два новых `@Component`-класса, ничего лишнего.

**Контекст.** BAGGAGE_LOST отличается от BAGGAGE_DELAYED:
- Порог: > 21 дня без возврата = официально "потерян" (Montreal, Article 17).
- Компенсация: фиксированный лимит Montreal ≈ 1131 SDR. Для MVP — 1000 EUR flat.
- Документы: PIR mandatory + BAG_TAG + желательно receipts за вещи.
- Письмо: Montreal Convention Article 17 (не 19), обращение к Baggage Claims Department.

**Задачи:**

1. **Расширить `IssueType`** (~5 мин)
   - Добавить `BAGGAGE_LOST` в enum.

2. **`BaggageLostEligibilityStrategy`** (~1.5 ч)
   ```java
   // supportedType() → BAGGAGE_LOST
   // eligible: !extraordinary && baggageDelayHours > 504  (21 * 24)
   // compensationAmount: 1000 EUR (flat, Montreal SDR лимит)
   // requiredDocuments: List.of(PIR, BAG_TAG)
   ```
   - Добавить `@Component`.

3. **`BaggageLostLetterStrategy`** (~45 мин)
   - Subject: `"Baggage Loss Compensation Claim — <flight> (<route>)"`.
   - Body: Montreal Convention Article 17, Baggage Claims Department, список вложений.
   - Добавить `@Component`.

4. **Тесты** (~1.5 ч)
   - `EligibilityServiceImplTest`: 3 юнит-теста (eligible > 504h, not eligible ≤ 504h, extraordinary).
   - `ClaimServiceImplTest.setUp()`: добавить обе стратегии в списки.
   - `ClaimIntegrationTest`: интеграционный тест с `buildBaggageLostIssue(600)` — eligible, letter содержит "Article 17".

5. **Прогон тестов + коммит** (~15 мин)
   - `./mvnw clean test`.
   - `feat(claim): add BAGGAGE_LOST claim type`.

**Acceptance criteria:**
- `BAGGAGE_LOST` проходит через весь API (create → eligibility → letter) без изменений в существующих сервисах.
- `EligibilityServiceImpl` и `ClaimLetterServiceImpl` не тронуты.
- Минимум 3 юнит-теста + 1 интеграционный.

**Ловушки:**
- Не забыть `@Component` на обоих классах.
- `baggageDelayHours` — реиспользуем существующее поле (как для BAGGAGE_DELAYED): смысл "часы с момента потери/розыска" тот же.
- Проверить порог: 21 день = 504 часа, condition `> 504` (строго больше).

---

## Day 2 — BAGGAGE_DAMAGED (Phase 2)

**Цель.** Последний тип кейса — повреждённый багаж. После этого Phase 2 закрыта (6/6).

**Контекст.** BAGGAGE_DAMAGED:
- Правовая база: Montreal Convention Article 17 §2.
- Срок подачи: **7 дней** с момента получения багажа (жёсткое ограничение).
- Компенсация: возмещение реального ущерба, лимит ≈ 1131 SDR. Для MVP — 800 EUR flat (разумная средняя по практике).
- Документы: PIR mandatory + PHOTO (фото повреждений).
- Письмо: Article 17 §2, Baggage Claims Department.

**Задачи:**

1. **Расширить `IssueType`** (~5 мин)
   - Добавить `BAGGAGE_DAMAGED` в enum.

2. **V3 миграция Flyway** (~20 мин)
   - `V3__add_baggage_damage_fields.sql`:
     ```sql
     alter table issues add column days_since_delivery integer;
     ```
   - Поле для проверки срока подачи (7 дней).
   - Добавить `daysSinceDelivery` в `Issue` entity + `IssueRequest` DTO.

3. **`BaggageDamagedEligibilityStrategy`** (~1.5 ч)
   ```java
   // supportedType() → BAGGAGE_DAMAGED
   // eligible: !extraordinary && daysSinceDelivery != null && daysSinceDelivery <= 7
   // compensationAmount: 800 EUR flat
   // requiredDocuments: List.of(PIR, PHOTO)
   ```
   - `@Component`.

4. **`BaggageDamagedLetterStrategy`** (~45 мин)
   - Subject: `"Baggage Damage Compensation Claim — <flight> (<route>)"`.
   - Body: Montreal Convention Article 17 §2, упомянуть срок подачи, Baggage Claims Department.
   - `@Component`.

5. **Тесты** (~1.5 ч)
   - `EligibilityServiceImplTest`: 4 теста (eligible ≤ 7 дней, not eligible > 7 дней, extraordinary, null daysSinceDelivery).
   - `ClaimServiceImplTest.setUp()`: обе стратегии.
   - `ClaimIntegrationTest`: интеграционный тест, letter содержит "Article 17" и "damage".

6. **Прогон тестов + коммит** (~15 мин)
   - `./mvnw clean test`.
   - `feat(claim): add BAGGAGE_DAMAGED claim type — Phase 2 complete`.

**Acceptance criteria:**
- Coverage: **6/6** типов (DELAY, CANCELLATION, MISSED_CONNECTION, BAGGAGE_DELAYED, BAGGAGE_LOST, BAGGAGE_DAMAGED).
- V3 миграция применяется в TestContainers автоматически.
- Все тесты зелёные.

**Ловушки:**
- Имя таблицы в миграции — `issues` (множ.).
- `daysSinceDelivery` без `@Column` — Hibernate авто-конвертит в `days_since_delivery`.
- `null`-проверка в `eligible`: если `daysSinceDelivery == null`, claim не eligible (нет данных → нельзя подтвердить срок).

---

## Day 3 — Auto-send letter to airline (Phase 1)

**Цель.** Сейчас при переходе в `SUBMITTED` уведомление уходит только пользователю. Претензия в авиакомпанию не отправляется — ключевой gap Phase 1. Это самый важный шаг: без него продукт генерирует письмо но никуда не отправляет.

**Контекст:**
- `ClaimLifecycleServiceImpl.transition` публикует `ClaimStatusTransitionedEvent`.
- `EmailNotificationService.onClaimTransitioned` уже реагирует на SUBMITTED.
- Сейчас `sendClaimSubmitted` шлёт письмо пользователю ("your claim has been submitted").
- Нужно: при SUBMITTED также отправить претензионное письмо (`ClaimLetterService.generateLetter`) на email авиакомпании.

**Задачи:**

1. **Airline email lookup** (~30 мин)
   - Добавить `Map<String, String> airlineEmails` в `EmailNotificationService` — простой словарь `airline name → email`.
   - Пример: `"Lufthansa" → "claims@lufthansa.com"`, `"default" → "claims@airline.com"`.
   - Альтернатива: `@Value`-список в `application.yaml`. Для MVP — hardcoded Map в конструкторе.

2. **`sendClaimLetterToAirline` метод** (~45 мин)
   - Добавить в `NotificationService` интерфейс: `void sendClaimLetterToAirline(Claim claim)`.
   - Реализовать в `EmailNotificationService`:
     - Получить `LetterResponse` через... проблема: `EmailNotificationService` не знает о `ClaimLetterService`.
     - Решение: инжектировать `ClaimLetterService` в `EmailNotificationService` или сделать это в `onClaimTransitioned` event payload'ом.
     - Чище: добавить `letterBody` в `ClaimStatusTransitionedEvent` (опционально) или инжектировать `ClaimLetterService`.
   - Тело письма = `LetterResponse.body` + From = `claim.getUser().getEmail()` (пассажир отправляет).

3. **Подключить к listener'у** (~20 мин)
   - В `onClaimTransitioned` при `event.to() == SUBMITTED` вызвать `sendClaimLetterToAirline(event.claim())` после `sendClaimSubmitted`.

4. **Тесты** (~1 ч)
   - `EmailNotificationServiceTest`: тест что при SUBMITTED вызывается `mailSender.send` дважды (один раз пользователю, один авиакомпании).
   - `ClaimIntegrationTest`: тест `submitClaim_sendsLetterToAirline` — `verify(mailSender, times(2)).send(any())`.

5. **Коммит** — `feat(notifications): auto-send claim letter to airline on SUBMITTED`.

**Acceptance criteria:**
- POST transition SUBMITTED → два email: один пользователю, один в авиакомпанию.
- Email авиакомпании содержит тело из `ClaimLetterService.generateLetter`.
- Если авиакомпания не в словаре — письмо на дефолтный адрес (graceful fallback, не NPE).

**Ловушки:**
- Circular dependency: если `EmailNotificationService` инжектирует `ClaimLetterService`, а тот где-то тянет notifications — будет цикл. Проверить до написания кода.
- `@TransactionalEventListener(AFTER_COMMIT)` — `ClaimLetterService` нужны данные claim, они уже persisted, EntityGraph не нужен (claim уже загружен в event).

---

## Day 4 — Follow-up scheduler (Phase 3 старт)

**Цель.** Первый шаг к автоматизации: если claim в статусе `SUBMITTED` больше 14 дней без ответа — автоматически перейти в `FOLLOW_UP_SENT` и послать follow-up письмо. Продукт становится агентом.

**Контекст:**
- Текущая модель: `FOLLOW_UP_SENT` ставится вручную через `/transition`.
- Нужно: cron-job проверяет раз в день — кандидаты на follow-up.
- Данные для проверки: `ClaimEvents` с `type=LETTER_SUBMITTED` (фиксируется при transition → SUBMITTED).

**Задачи:**

1. **Добавить `updatedAt` в `Claim`** (~20 мин) или использовать `ClaimEvents`.
   - Лучше читать из `ClaimEvents`: найти последнее событие `LETTER_SUBMITTED` и сравнить `createdAt` с `now() - 14 days`.
   - Добавить в `EventsRepository`: `List<Long> findClaimIdsEligibleForFollowUp(LocalDateTime threshold)` (native или JPQL-запрос).

2. **`FollowUpSchedulerService`** (~1.5 ч)
   ```java
   @Service
   @RequiredArgsConstructor
   public class FollowUpSchedulerService {
       private final ClaimRepository claimRepository;
       private final EventsRepository eventsRepository;
       private final ClaimService claimService;
       // ...
       @Scheduled(cron = "0 0 9 * * *")  // каждый день в 09:00
       @Transactional
       public void runFollowUpCheck() {
           // найти SUBMITTED claims старше 14 дней
           // для каждого: claimService.transition(id, FOLLOW_UP_SENT)
       }
   }
   ```
   - Включить `@EnableScheduling` в `ClaimsMvpApplication`.
   - Для dev: добавить `spring.task.scheduling.enabled=true` в properties (по умолчанию включено).

3. **Тест на scheduler** (~1 ч)
   - Юнит-тест `FollowUpSchedulerServiceTest`: создать claim с SUBMITTED-событием > 14 дней назад, вызвать `runFollowUpCheck()` напрямую, verify что `claimService.transition` был вызван.
   - Важно: тест вызывает метод напрямую (не через cron), mock `ClaimService`.

4. **dev-режим: форсированный запуск** (~20 мин)
   - Добавить `@GetMapping("/admin/scheduler/follow-up")` — ручной trigger для dev-проверки.
   - Только в dev-профиле.

5. **Коммит** — `feat(scheduler): auto follow-up for SUBMITTED claims after 14 days`.

**Acceptance criteria:**
- Scheduler запускается по расписанию.
- Claim в SUBMITTED > 14 дней → автоматически переходит в FOLLOW_UP_SENT.
- Follow-up email уходит (через существующий `EmailNotificationService`).
- Unit-тест покрывает логику выбора кандидатов.

**Ловушки:**
- `@Scheduled` + `@Transactional` в одном классе — может не работать из-за Spring proxy. Лучше: scheduler-метод без `@Transactional`, транзакция внутри вызываемого сервиса.
- Idempotency: если scheduler запустился дважды подряд (перезапуск), один claim не должен получить два follow-up. Проверка: после перехода в FOLLOW_UP_SENT он уже не в SUBMITTED — повторный запуск его не подхватит.
- Для prod — нужен `ShedLock` (multi-instance). Для MVP/dev — не нужен.

---

## Day 5 — Frontend → Real API (Phase 5 старт)

**Цель.** Подключить `app.html` к реальному бэкенду. Вместо хардкоженных данных — `fetch('/api/claims')`. Это делает прототип демо-пригодным.

**Контекст:**
- `app.html` лежит в `static/` — Spring Boot отдаёт его как статику напрямую.
- API доступен на том же origin → CORS не нужен.
- Эндпоинты: `GET /api/claims?page=0&size=20`, `GET /api/claims/{id}`, `GET /api/claims/{id}/letter`, `POST /api/claims/{id}/transition`.

**Задачи:**

1. **Загрузка списка claims** (~1 ч)
   - Заменить `CLAIMS` массив на `async function fetchClaims()` → `fetch('/api/claims?size=20')`.
   - Парсить `response.content[]` (Page<ClaimResponse>).
   - Показывать loading-state пока грузится.

2. **Детальная панель — реальные данные** (~45 мин)
   - При клике на строку: `fetch('/api/claims/{id}')` для метаданных + `fetch('/api/claims/{id}/letter')` для письма.
   - `Promise.all([...])` чтобы не ждать последовательно.

3. **Submit из UI** (~30 мин)
   - Кнопка "Submit claim" в детальной панели делает `POST /api/claims/{id}/transition` с `{ status: "SUBMITTED", note: "submitted from UI" }`.
   - После успеха — перезагружать детальную панель.

4. **Error handling** (~30 мин)
   - 409 при неверном переходе → показывать inline сообщение в панели.
   - 404 → "Claim not found".
   - Network error → retry-сообщение.

5. **Dashboard stats** (~30 мин)
   - `GET /api/claims?size=100` → считать статистику из response (или добавить `/api/claims/stats` эндпоинт — опционально).

6. **Коммит** — `feat(frontend): connect app.html to real API`.

**Acceptance criteria:**
- `localhost:8080/app.html` показывает реальные claims из БД.
- Детальная панель показывает реальное письмо из `ClaimLetterService`.
- Кнопка Submit меняет статус через API.
- Ошибки обрабатываются (не белый экран).

**Ловушки:**
- `Page<ClaimResponse>` возвращает Spring-формат с `content`, `totalPages` и т.д. — парсить именно `data.content`.
- Dates из API — ISO-строки, не JS Date объекты. `new Date(str).toLocaleDateString()` для отображения.
- После Submit кнопка должна блокироваться (disable) чтобы не слать два раза.

---

## Что в конце Week 2

- ✅ Phase 2 **полностью закрыта**: 6/6 типов кейсов.
- ✅ Phase 1 **значимо продвинута**: письмо реально уходит в авиакомпанию.
- ✅ Phase 3 **старт**: первый автоматический триггер работает.
- ✅ Phase 5 **старт**: фронт подключён к реальному API.

После Week 2: Phase 3 (escalation, deadline tracking), Phase 1 (inbound email parsing), Phase 4 (AI layer).
