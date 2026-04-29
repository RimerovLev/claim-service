# Roadmap — короткая выжимка

Полный план: [roadmap.md](roadmap.md)

## Приоритеты для одного разработчика / маленькой команды

1. **Phase 1 — Communications** (4-6 нед)
   Email-провайдер + автоматическая отправка претензий + нотификации пользователю.
   Без этого FSM, follow-up статусы и аудит — мёртвый код, письма никуда не уходят.

2. **Phase 2 — Coverage breadth** (4-6 нед, параллелится с Phase 1)
   Добавить 4 типа кейсов кроме DELAY/CANCELLATION: missed connection, delayed/lost/damaged baggage, denied boarding. Удваивает (минимум) addressable market.

3. **Phase 3 — Automation / scheduler** (3-4 нед, после Phase 1)
   Job scheduler + автоматические follow-up по таймерам + escalation paths + deadline tracking. Превращает FSM из «руками двигаемого» в реального агента.

4. **Phase 5 — Frontend на Next.js** (4-8 нед)
   Без фронта продукт нельзя продавать. Можно параллелить с самого начала, если есть отдельная команда.

5. **Phase 4 — AI / LLM-слой** (4-6 нед)
   Главная фишка из названия проекта. Адаптивная генерация писем, объяснения для пользователя, анализ ответов авиакомпаний. Без Phase 1 и 3 ей не на чём проявиться.

6. **Phase 6 — Analytics + admin** (3-4 нед)
   Funnel-метрики, сегментация, admin-панель для case manager'а.

7. **Phase 7 — SEO / Content**
   Continuous, после Phase 5.

8-9. **Phase 8/9 — Scale + generic engine** — long-term.

---

## Главная рекомендация

**Следующий шаг — Phase 1 (communications).**

Причины:
- Разблокирует Phase 3 (automation требует, чтобы письма реально уходили).
- Разблокирует Phase 4 (inbound email parsing для AI-анализа ответов).
- Закрывает требование ТЗ про логирование отправленных писем.

Параллельно можно стартовать Phase 2 (coverage) — она независимая.

---

## Тех-долг перед фазами

**Перед Phase 2:** разбить `EligibilityServiceImpl.evaluate` и `ClaimLetterServiceImpl` на стратегии по `IssueType` (`Map<IssueType, EligibilityStrategy>`). Иначе добавление 4 новых типов сделает классы грязными.

**Перед Phase 3:** идемпотентность scheduler'а, отдельная таблица `scheduled_actions` для аудита.

**Перед Phase 8:** перенос storage на S3 (абстракция уже есть — `DocumentStorageService`); миграция `events.payload` с `TEXT` на `jsonb` для аналитики.
