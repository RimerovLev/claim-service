# claims-mvp

## Agent handoff

**Read this first if you are a new model/session:** [docs/agent-handoff.md](docs/agent-handoff.md). It captures working format, tone, current state, and common pitfalls.

## Where we are right now

**Latest day:** [docs/daily/2026-05-19.md](docs/daily/2026-05-19.md)
**Current sprint:** [docs/week-plan.md](docs/week-plan.md)
**Architecture map:** [docs/architecture-overview.md](docs/architecture-overview.md)
**Roadmap (with progress):** [docs/roadmap.md](docs/roadmap.md)
**TZ vs implementation:** [docs/tz-checklist.md](docs/tz-checklist.md)

When starting a new session, read agent-handoff first, then this file, then the latest daily snapshot.

## Stack

Java 21, Spring Boot 4, PostgreSQL, Flyway, MapStruct, Lombok, TestContainers, Spring Security + JWT, Spring Mail, Jackson 3 (`tools.jackson.databind`).

Frontend: React 19 + Vite + TypeScript + React Router v7 (`/frontend/`).

## Package structure

```
com.claims.mvp
├── claim/                      core domain
│   ├── controller/             ClaimController, DocumentController
│   ├── service/lifecycle/      ClaimLifecycleServiceImpl  ← orchestrator
│   ├── service/workflow/       ClaimWorkflowServiceImpl   ← FSM
│   ├── service/documents/      ClaimDocumentsServiceImpl
│   ├── service/storage/        DocumentStorageServiceImpl ← file storage + MIME
│   └── service/letter/         ClaimLetterServiceImpl + 6 LetterStrategy impls
├── eligibility/                pure rule engine
│   ├── service/                EligibilityServiceImpl (delegator)
│   └── strategy/               EligibilityStrategy + 6 impls (Delay, Cancellation,
│                                  MissedConnection, Baggage{Delayed,Lost,Damaged})
├── notifications/              event-driven email
│   ├── NotificationService, EmailNotificationService
│   └── events/                 ClaimCreatedEvent, ClaimStatusTransitionedEvent
├── scheduler/                  FollowUpSchedulerService (@Scheduled cron 9:00)
├── security/                   SecurityConfig, JwtAuthFilter, JwtService, AuthController
├── events/                     ClaimEvents audit log
├── user/                       UserController, AdminUserController, Role enum
├── exception/                  GlobalExceptionHandler, DuplicateUserException
└── web/                        HomeController (Thymeleaf /)
```

## Key architecture rules

- **`ClaimLifecycleServiceImpl`** orchestrates all claim operations. Controllers never call other services directly.
- **`ClaimWorkflowServiceImpl`** is the only place that reads/mutates `ClaimStatus`. No `if (claim.getStatus() == ...)` elsewhere.
- **`EligibilityServiceImpl`** is pure — no I/O. Delegates to `EligibilityStrategy` per `IssueType`.
- **`ClaimLetterServiceImpl`** — same pattern via `LetterStrategy`.
- **All strategies are `@Component`.** Adding a new claim type = one new strategy class. Services are not touched.
- **Notifications go through events** (`ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`). Lifecycle does not know about email.
- **Ownership in service layer** (lifecycle/storage), not in controllers. `@PreAuthorize` for role-checks; ownership is a runtime check (`assertOwnerOrAdmin`).
- **Scheduled methods** must set a synthetic `SecurityContext` before invoking `@PreAuthorize`-protected services, then `clearContext` in `finally`.
- **Migrations via Flyway only.** Never `ddl-auto=create/update` in prod. `ddl-auto=create-drop` is OK in tests via TestContainers.
- **MapStruct for all DTO ↔ entity.** No manual mapping code.

## Security model

- JWT stateless (`SecurityConfig` chain + `JwtAuthFilter`).
- 3 roles: `USER`, `MODERATOR`, `ADMIN` (in `Role` enum, stored on `User`).
- Public: `/api/auth/**`, `/`, `/app.html`, static.
- `@EnableMethodSecurity` — `@PreAuthorize` enforced on every controller method.
- USER sees only own claims (ownership check). ADMIN sees all. MODERATOR is currently treated like USER for ownership — open product question.
- Tests: `IntegrationTestBase` sets `app.security.enabled=false` via `@DynamicPropertySource`. Production `SecurityConfig` is then excluded via `@ConditionalOnProperty(matchIfMissing=true)`. `TestSecurityConfig` (loads on `havingValue="false"`) provides permit-all chain + `PasswordEncoder` bean.

## Known issues (do not introduce workarounds)

- `BoardingDocuments.deletedAt` field exists but no soft-delete logic implemented.
- `ClaimEvents.payload` stored as `TEXT`; should be `jsonb` before analytics work.
- `Claim` `@OneToOne` associations are technically `EAGER` despite `fetch = LAZY` (Hibernate non-owning side limitation; needs bytecode enhancement plugin).
- `MODERATOR` role is not handled in `assertOwnerOrAdmin` — only `ROLE_ADMIN` bypasses ownership. Product decision pending.
- `PasswordEncoder` bean lives in `SecurityConfig` and disappears when production config is disabled in tests; `TestSecurityConfig` redeclares it. Should move to a separate `CryptoConfig`.
- Frontend has no auth integration — no JWT in headers, no login UI yet.
- Outbound email is only sent to user, not to airline at SUBMITTED. Closing this is sprint-1 priority.
- Local file storage (`uploads/`), not S3. Need to migrate before scale.

## Testing rules

- Integration tests extend `IntegrationTestBase` (TestContainers Postgres + `app.security.enabled=false`).
- Unit tests under `ClaimServiceImplTest` and `EligibilityServiceImplTest` use Mockito; security context is set in `@BeforeEach` and cleared in `@AfterEach`.
- WebMvcTest for controllers uses `@MockitoBean` (Spring Boot 4) — not deprecated `@MockBean`.
- Do not mock `NotificationService` in integration tests — it kills the `@TransactionalEventListener` registration. Mock `JavaMailSender` instead.

## What to always use

- `@RequiredArgsConstructor` + `final` fields for injection (no `@Autowired`).
- `Optional.orElseThrow(EntityNotFoundException::new)` for repo lookups.
- `ClaimStatus` / `IssueType` / `EventTypes` enums — never raw strings.
- `Boolean.TRUE.equals(field)` to safely compare nullable `Boolean` (don't rely on autounbox).
