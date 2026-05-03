# claims-mvp

## Where we are right now

**Latest day:** [docs/daily/2026-05-03.md](docs/daily/2026-05-03.md)
**Current week plan:** [docs/week-plan.md](docs/week-plan.md)
**Architecture map:** [docs/architecture-overview.md](docs/architecture-overview.md)
**Long-term roadmap:** [docs/roadmap.md](docs/roadmap.md)
**TZ vs implementation:** [docs/tz-checklist.md](docs/tz-checklist.md)

When starting a new session, read the latest daily snapshot first.

## Stack
Java 21, Spring Boot 4, PostgreSQL, Flyway, MapStruct, Lombok, TestContainers

## Package structure
```
com.claims.mvp
├── claim/          → core domain (controller, service, dao, dto, model, mapper, enums)
│   ├── service/lifecycle/   ClaimLifecycleServiceImpl  ← orchestrator (god node, 17 edges)
│   ├── service/workflow/    ClaimWorkflowServiceImpl   ← FSM transitions
│   ├── service/documents/   ClaimDocumentsServiceImpl  ← document management
│   ├── service/storage/     DocumentStorageServiceImpl ← file persistence + MIME validation
│   └── service/letter/      ClaimLetterServiceImpl     ← delegator over LetterStrategy beans
├── eligibility/    → pure rule engine, no side effects
│   ├── service/             EligibilityServiceImpl (delegator over strategies)
│   └── strategy/            EligibilityStrategy + per-IssueType impls (Delay, Cancellation)
├── events/         → ClaimEvents audit log
├── user/           → UserController → UserServiceImpl → UserRepository
├── exception/      → GlobalExceptionHandler + ErrorResponse record
└── web/            → HomeController (Thymeleaf home page)
```

## Key architecture rules
- **ClaimLifecycleServiceImpl** orchestrates all claim state changes — it calls WorkflowService, DocumentsService, EligibilityService, LetterService. Never bypass it from the controller.
- **FSM transitions** live exclusively in `ClaimWorkflowServiceImpl`. Never check or mutate `ClaimStatus` outside this class.
- **EligibilityService** is a pure function — no DB writes, no side effects. Keep it that way.
- **MapStruct mappers** for all DTO ↔ entity conversions. Never write manual mapping code.
- **Flyway only** for schema changes. Never use `ddl-auto=create` or `update`.

## Known issues (do not introduce workarounds)
- `BoardingDocuments.deletedAt` field exists but no soft-delete logic implemented (repository does not filter, no service writes the timestamp)
- `ClaimEvents.payload` is stored as `TEXT`; consider migrating to `jsonb` before analytics work
- `Claim` `@OneToOne` associations are technically `EAGER` despite `fetch = LAZY` annotation (Hibernate limitation on non-owning side; needs bytecode enhancement plugin to truly lazy-load)

## Testing rules
- Integration tests extend `IntegrationTestBase` which spins up a real PostgreSQL via TestContainers
- Unit tests in `ClaimServiceImplTest` and `EligibilityServiceImplTest` — mock all dependencies
- Do not modify tests unless the task explicitly requires it
- Do not add `@MockBean` where the real bean works fine with TestContainers

## What to always use
- `@RequiredArgsConstructor` + `final` fields for injection (no `@Autowired`)
- `Optional.orElseThrow(EntityNotFoundException::new)` for repo lookups
- `ClaimStatus` enum for all status references — never use raw strings
