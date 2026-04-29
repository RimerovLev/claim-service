# План на неделю — claims-mvp

5 рабочих дней. Каждый день — самодостаточный чанк: в конце всё компилится, тесты зелёные, можно коммитить.

Цель недели: причесать тех-долг, добавить 2 новых типа кейсов (missed connection + baggage delayed), запустить первый кусок communications layer.

---

## Day 1 — Cleanup + начало рефакторинга на стратегии

**Цель.** Зафиксировать предыдущую работу коммитом и подготовить почву для расширения типов кейсов.

**Задачи:**

1. **Cleanup дебага и закрытие предыдущего рефакторинга** (~30 мин)
   - Удалить `@Slf4j` + `log.error(...)` из `GlobalExceptionHandler.handleRuntimeException` (это был временный дебаг).
   - `./mvnw clean test` — убедиться что всё зелёное.
   - `git add -A && git commit` с текстом из `docs/commit-message` (если хочешь, могу сгенерить).

2. **Начало рефакторинга `EligibilityService` на стратегии** (~3 часа)
   - Создать интерфейс `com.claims.mvp.eligibility.strategy.EligibilityStrategy`:
     ```java
     public interface EligibilityStrategy {
         IssueType supportedType();
         EligibilityResult evaluate(Issue issue, Flight flight, EuContext euContext, List<BoardingDocuments> documents);
     }
     ```
   - Создать классы `DelayEligibilityStrategy`, `CancellationEligibilityStrategy` — перенести логику из текущего `EligibilityServiceImpl.evaluate` в соответствующие методы. Каждый класс — `@Component` с `@Order` или просто Spring-бин.
   - В `EligibilityServiceImpl` инжектить `List<EligibilityStrategy>` (Spring сам соберёт все бины), строить `Map<IssueType, EligibilityStrategy>` в конструкторе через stream'ы.
   - `evaluate(...)` теперь делает:
     ```java
     EligibilityStrategy strategy = strategiesByType.get(issue.getType());
     if (strategy == null) throw new IllegalArgumentException("No strategy for " + issue.getType());
     return strategy.evaluate(issue, flight, euContext, documents);
     ```

**Acceptance criteria:**
- Все существующие тесты `EligibilityServiceImplTest` проходят без изменений.
- `EligibilityServiceImpl` стал thin-делегатом.
- Логика DELAY и CANCELLATION разнесена по двум классам.

**Risks / гэпы:**
- `calculateCompensationAmount` сейчас в `EligibilityServiceImpl` — вынести его в utility или общий abstract base для стратегий.

---

## Day 2 — Стратегии для `ClaimLetterService` + регрессия

**Цель.** Доделать рефакторинг: разнести генерацию писем по стратегиям, как сделали для eligibility.

**Задачи:**

1. **`LetterStrategy` интерфейс** (~2 часа)
   - Создать `com.claims.mvp.claim.service.letter.strategy.LetterStrategy`:
     ```java
     public interface LetterStrategy {
         IssueType supportedType();
         LetterResponse generate(Claim claim);
     }
     ```
   - `DelayLetterStrategy` и `CancellationLetterStrategy` — перенести соответствующие куски `switch` из текущего `ClaimLetterServiceImpl`.
   - `ClaimLetterServiceImpl` инжектит `List<LetterStrategy>`, строит мапу, делегирует.

2. **Регрессия** (~1 час)
   - Прогнать все тесты `./mvnw clean test`.
   - Если что-то падает — проверить инициализацию мапы стратегий, особенно в юнит-тестах с `@Mock`. Возможно, придётся вручную создавать стратегии в `setUp()` (как мы делали для `ClaimWorkflowServiceImpl`).

3. **Документация в коде** (~30 мин)
   - Краткий Javadoc на `EligibilityStrategy` и `LetterStrategy` — пояснить, что новый тип кейса добавляется через создание новой стратегии.

**Acceptance criteria:**
- `ClaimLetterServiceImpl` без `switch (issue.getType())`.
- Все интеграционные тесты зелёные.
- Юнит-тесты адаптированы.

**Buffer:** если день закончится раньше — начать Day 3 (research правил для missed connection).

---

## Day 3 — Missed connection

**Цель.** Добавить третий тип кейса end-to-end. На нём проверим, что архитектура стратегий действительно даёт лёгкое расширение.

**Задачи:**

1. **Расширить enum** (~10 мин)
   - `IssueType.MISSED_CONNECTION`.
   - `Issue.connectionDelayMinutes` или отдельные поля (зависит от правил).

2. **Eligibility-стратегия** (~2 часа)
   - Правила EU 261 для missed connection: компенсация полагается, если стыковка пропущена из-за задержки/отмены первого сегмента, итоговое опоздание ≥ 3 часов.
   - Новый `MissedConnectionEligibilityStrategy` с этими правилами.
   - Required documents — те же что для DELAY/CANCELLATION плюс boarding pass второго сегмента.

3. **Letter-стратегия** (~1 час)
   - `MissedConnectionLetterStrategy` — шаблон письма с упоминанием обоих сегментов и расчёта итоговой задержки.

4. **Тесты** (~2 часа)
   - Юнит-тесты для `MissedConnectionEligibilityStrategy`: eligible / not eligible / extraordinary.
   - Интеграционный тест: создать claim, проверить eligibility и компенсацию.
   - Тест на letter generation с правильным форматом.

**Acceptance criteria:**
- Новый тип кейса работает через тот же API, что DELAY/CANCELLATION.
- 3+ юнит-теста для стратегии.
- 1 интеграционный тест для full flow.

**Risks:**
- Правила missed connection в EU 261 не такие очевидные. Если есть сомнения — посмотреть статью 5 регулирования или просто покрыть базовый сценарий.

---

## Day 4 — Baggage delayed (первый багажный кейс)

**Цель.** Расширить продукт за пределы EU 261 — багажные кейсы регулирует Montreal Convention. Это другая правовая база, другая логика компенсации (не таблица по дистанции, а лимит ~1300 SDR).

**Задачи:**

1. **Расширить модель** (~1 час)
   - `IssueType.BAGGAGE_DELAYED`.
   - `Issue.baggageDelayHours` или поля под багажные обстоятельства.
   - `DocumentTypes` уже содержит `PIR`, `BAG_TAG`, `PHOTO` — проверить, не нужны ли новые (например, `RECEIPTS` для расходов на замену вещей).

2. **Eligibility-стратегия** (~2.5 часа)
   - `BaggageDelayedEligibilityStrategy`:
     - Eligibility: если задержка ≥ 21 день — потеря (другой кейс), если меньше — delay.
     - Required documents: PIR mandatory, receipts, baggage tag.
     - Компенсация: возмещение reasonable expenses в пределах ~1300 SDR (для MVP — простая модель: до X EUR без чеков, по чекам — что меньше).
   - Compensation calculation отличается принципиально — не привязан к distanceKm.

3. **Letter-стратегия** (~1 час)
   - `BaggageDelayedLetterStrategy`: упомянуть Montreal Convention статью 19, перечислить документы.

4. **Тесты** (~2 часа)
   - Юнит-тесты на стратегию: разные дни задержки, наличие/отсутствие чеков.
   - Интеграционный тест.

**Acceptance criteria:**
- Coverage расширен до 4 типов: DELAY, CANCELLATION, MISSED_CONNECTION, BAGGAGE_DELAYED.
- Тесты зелёные.

**Risks:**
- SDR/EUR конвертация — для MVP можно захардкодить курс или взять хардкод-таблицу. Реальный курс через API — задача для Phase 8.
- `Flight.distanceKm` для багажных кейсов нерелевантен — eligibility-стратегия должна это учитывать.

---

## Day 5 — Email starter (Phase 1, первый кусок)

**Цель.** Запустить базовую email-инфраструктуру: при создании claim уходит уведомление пользователю. Это первый шаг к реальному «агенту, который что-то делает».

**Задачи:**

1. **Email-конфиг** (~1 час)
   - Добавить зависимость `spring-boot-starter-mail` в `pom.xml`.
   - В `application.yaml`/`.properties` настроить SMTP (на локальной разработке — MailHog или Mailpit, в Docker; для prod — placeholder под SendGrid/SES).
   - Профили `dev` (MailHog) и `prod` (env vars).

2. **`NotificationService` интерфейс + Email-реализация** (~2 часа)
   - `com.claims.mvp.notifications.NotificationService`:
     ```java
     public interface NotificationService {
         void sendClaimCreated(Claim claim);
         void sendClaimSubmitted(Claim claim);
     }
     ```
   - `EmailNotificationService implements NotificationService` — собирает текст из шаблонов и отправляет через `JavaMailSender`.
   - Шаблоны: для MVP можно прямо в коде или Thymeleaf templates.

3. **Хук в lifecycle** (~30 мин)
   - В `ClaimLifecycleServiceImpl.createClaim` вызывать `notificationService.sendClaimCreated(claim)` после `save`.
   - В `ClaimLifecycleServiceImpl.transition` при target=SUBMITTED — `sendClaimSubmitted(claim)`.

4. **Локальное тестирование** (~1 час)
   - Запустить MailHog локально (`docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog`).
   - Создать claim через `POST /api/claims`, проверить что письмо появилось в MailHog UI на `localhost:8025`.

5. **Интеграционные тесты** (~1.5 часа)
   - Подменить `NotificationService` на mock в тестах, чтобы не слать реальные письма.
   - Verify mock был вызван при `createClaim` и `transition` к SUBMITTED.

**Acceptance criteria:**
- Создание claim → пользователь получает email.
- Submission → письмо «ваше обращение отправлено» (для MVP — пользователю, не в авиакомпанию ещё).
- Тесты mock'ают NotificationService.

**Buffer:**
- Если время есть — добавить inbound email mock, чтобы заложить почву для AI Phase 4.
- Шаблоны можно подкрасить через Thymeleaf, но не обязательно.

---

## Что в конце недели

После 5 дней:
- ✅ Тех-долг рефакторинга снят, новый тип добавляется через 1 файл-стратегию.
- ✅ Coverage: 4 типа кейсов вместо 2 (66% от целевых 6 в ТЗ).
- ✅ Communications: первая отправка email пользователю работает, инфраструктура готова к расширению.
- ✅ Архитектурно: видно как добавлять новые типы и новые типы нотификаций.

Не сделано в этой неделе (на следующие итерации):
- Auto-send претензии **в авиакомпанию** (нужно решить — отправлять напрямую или только готовить).
- Inbound email parsing.
- Scheduler для follow-up.
- Денежные кейсы (lost / damaged baggage) — добавить за 1-1.5 дня по аналогии с delayed.
- Frontend.

---

## Заметки про режим работы

- Каждый день стартует с `git pull` + `./mvnw clean test`.
- Каждый день заканчивается коммитом со зелёными тестами.
- Если день не укладывается — переносим хвост на буфер следующего дня. Не накапливать «полу-готовое».
- Если что-то идёт сильно не так (например, на Day 4 правила Montreal оказываются сложнее, чем ожидалось) — лучше временно сократить scope до базового сценария, закоммитить, разобрать остаток отдельно.
