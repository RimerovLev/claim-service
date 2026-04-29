# claims-mvp

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
│   └── service/letter/      ClaimLetterServiceImpl     ← EU 261/2004 letter generation
├── eligibility/    → EligibilityServiceImpl (pure rule engine, no side effects)
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
- `BoardingDocumentsRepository` has no soft-delete filter — `deletedAt` is not applied automatically
- `UserServiceImpl` has a potential race condition on user creation (no unique constraint handling)
- `DocumentStorageServiceImpl` — magic-bytes MIME check exists but error path incomplete

## Testing rules
- Integration tests extend `IntegrationTestBase` which spins up a real PostgreSQL via TestContainers
- Unit tests in `ClaimServiceImplTest` and `EligibilityServiceImplTest` — mock all dependencies
- Do not modify tests unless the task explicitly requires it
- Do not add `@MockBean` where the real bean works fine with TestContainers

## What to always use
- `@RequiredArgsConstructor` + `final` fields for injection (no `@Autowired`)
- `Optional.orElseThrow(EntityNotFoundException::new)` for repo lookups
- `ClaimStatus` enum for all status references — never use raw strings
