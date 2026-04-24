# Claims MVP — Progress Checklist (Printable)

Дата: 2026-04-13  
Проект: `claims-mvp`  

Легенда:
- `[x]` — сделано
- `[~]` — частично (есть, но нужно улучшить/доделать)
- `[ ]` — не сделано

## MVP
- [x] Форма создания кейса (`POST /api/claims`)
- [~] Выбор типа проблемы (есть: `DELAY`, `CANCELLATION`; нет: missed connection / baggage cases)
- [x] Ввод данных рейса и бронирования (flight + bookingRef)
- [~] Загрузка подтверждающих документов (есть `type + url`, нет реального upload/storage)
- [x] Базовая проверка права на компенсацию (rule-based для delay/cancellation + EU scope + extraordinary)
- [x] Генерация претензии / обращения
- [ ] Отправка пользователю готового письма или авто-отправка
- [x] Сохранение кейса в “CRM” (таблицы + `GET /api/claims`, `GET /api/claims/{id}`)
- [ ] Базовые follow-up напоминания
- [ ] Аналитика по статусам кейсов

## 1) Intake / Claim Intake Layer
- [x] Создание кейса пользователем
- [x] Сбор данных рейса/бронирования
- [~] Загрузка файлов (пока только ссылки на документы)
- [~] Определение категории кейса (пока только через `IssueType`)

## 2) Eligibility / Rules Engine
- [x] Rule-based eligibility (минимальный)
- [x] Delay compensation (>= 180 мин)
- [x] Cancellation compensation (notice <= 14 дней)
- [ ] Denied boarding
- [ ] Missed connection
- [ ] Delayed baggage
- [ ] Lost baggage
- [ ] Damaged baggage
- [~] Юрисдикции/ограничения (минимально: EU scope + extraordinary)
- [x] Required documents (минимально: Ticket + Boarding pass для flight claims)

## 3) AI-assistant Layer
- [ ] Объяснение пользователю (почему eligible / не eligible)
- [ ] Генерация претензии (email/web/PDF)
- [ ] Генерация follow-up писем
- [ ] Escalation шаблоны

## 4) Claim Workflow / Automation
- [x] Статусы + allowed transitions (FSM)
- [x] Авто-статус `DOCS_REQUESTED` ↔ `READY_TO_SUBMIT` по наличию required docs (pre-submit зона)
- [x] Логирование статусов через events (`claim_events`)
- [ ] Автоматические follow-up по времени (scheduler/queue)
- [ ] Escalated (как отдельный статус + правила/таймеры)

## 5) CRM-блок
- [x] Карточка пользователя (минимально: create user)
- [x] Карточка claim (в БД + API)
- [~] История кейсов по пользователю (пока нет endpoint `GET /api/users/{id}/claims`)
- [ ] Сегментация/фильтры/поиск

## 6) Communications Layer
- [ ] Email-уведомления
- [ ] Telegram/WhatsApp/push
- [ ] Полуавтоматическая/автоматическая отправка претензий

## 7) Content / SEO Layer
- [ ] SEO-страницы
- [ ] Образовательный контент

## 8) Integrations Layer
- [ ] Email provider
- [ ] Storage (S3/MinIO)
- [ ] Flight status API
- [ ] OCR / document parsing
- [ ] E-sign / PDF generation

## 9) Analytics
- [ ] Метрики по кейсам (создано/успешно/суммы/конверсии)
- [ ] Dashboard

## Tech / Engineering (что уже сделано)
- [x] Единая конвенция DTO: `dto.request` / `dto.response`
- [x] MapStruct (ModelMapper удалён)
- [x] Controller возвращает объект (без `ResponseEntity`)
- [x] Тесты: unit + web layer + integration (Testcontainers Postgres)
- [x] Миграции Flyway/Liquibase (вместо `ddl-auto`)
- [ ] Auth/JWT + привязка claims к текущему пользователю (не через `userId`)

