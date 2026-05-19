# Roadmap — короткая выжимка

Полный план: [roadmap.md](roadmap.md). Текущий спринт: [week-plan.md](week-plan.md).

## Где мы сейчас (2026-05-10)

Backend MVP+ закрыт. Frontend — скелет.

- ✅ 6 типов кейсов, eligibility и letter через стратегии.
- ✅ FSM (10 статусов) + аудит-лог.
- ✅ Email пользователю + event-driven архитектура.
- ✅ Cron follow-up.
- ✅ Spring Security + JWT + 3 роли + ownership.
- ✅ Frontend SPA скелет (React 19 + Vite + TS).

## Главный gap

**Письмо в авиакомпанию не отправляется.** Сейчас email уходит только пользователю.
Без этого продукт ≠ агент, а просто генератор писем.

## Приоритет №1 — Спринт 1

1. **Outbound email в airline** при переходе SUBMITTED.
2. **Frontend auth integration** (login/register UI + JWT в headers).
3. **Claim detail page** на фронте.

После этого: пассажир регистрируется → создаёт claim → автоматически уходит письмо в airline → scheduler сам делает follow-up.

## Дальше (Sprint 2-4)

- **Sprint 2:** ESCALATED logic, deadline tracking, inbound email parsing, ShedLock.
- **Sprint 3:** AI/LLM layer — explanations, adaptive letters, inbound classification.
- **Sprint 4:** S3 migration, jsonb для payload, analytics dashboard, 152-ФЗ.

## Что не в фокусе сейчас

- Telegram/WhatsApp нотификации.
- DENIED_BOARDING (7-й тип кейса).
- 2FA / anomaly detection.
- SEO / контент-страницы.
- Generic claims engine (train/hotel/insurance).
