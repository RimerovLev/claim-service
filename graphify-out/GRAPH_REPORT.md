# Graph Report - .  (2026-04-28)

## Corpus Check
- Corpus is ~9,172 words - fits in a single context window. You may not need a graph.

## Summary
- 395 nodes · 577 edges · 65 communities detected
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 76 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Claim Controller Layer|Claim Controller Layer]]
- [[_COMMUNITY_Document Storage|Document Storage]]
- [[_COMMUNITY_Integration Tests|Integration Tests]]
- [[_COMMUNITY_Claim Request DTOs|Claim Request DTOs]]
- [[_COMMUNITY_Claim Service Tests|Claim Service Tests]]
- [[_COMMUNITY_Test Infrastructure|Test Infrastructure]]
- [[_COMMUNITY_Claim Repositories & Mappers|Claim Repositories & Mappers]]
- [[_COMMUNITY_Eligibility Service Interface|Eligibility Service Interface]]
- [[_COMMUNITY_Claim Service Interface|Claim Service Interface]]
- [[_COMMUNITY_Claim Controller Interface|Claim Controller Interface]]
- [[_COMMUNITY_User Domain|User Domain]]
- [[_COMMUNITY_Document Storage Service|Document Storage Service]]
- [[_COMMUNITY_Global Exception Handler|Global Exception Handler]]
- [[_COMMUNITY_Document Controller|Document Controller]]
- [[_COMMUNITY_Document Service|Document Service]]
- [[_COMMUNITY_User Mapper|User Mapper]]
- [[_COMMUNITY_Eligibility Engine|Eligibility Engine]]
- [[_COMMUNITY_Document Mapper|Document Mapper]]
- [[_COMMUNITY_Claim Workflow FSM|Claim Workflow FSM]]
- [[_COMMUNITY_App Context Test|App Context Test]]
- [[_COMMUNITY_Integration Test Base|Integration Test Base]]
- [[_COMMUNITY_Spring Boot Entry Point|Spring Boot Entry Point]]
- [[_COMMUNITY_Home Controller|Home Controller]]
- [[_COMMUNITY_User Controller|User Controller]]
- [[_COMMUNITY_User Service Interface|User Service Interface]]
- [[_COMMUNITY_Claim Entity|Claim Entity]]
- [[_COMMUNITY_Letter Service Interface|Letter Service Interface]]
- [[_COMMUNITY_User Response DTO|User Response DTO]]
- [[_COMMUNITY_Create User Request DTO|Create User Request DTO]]
- [[_COMMUNITY_User Model|User Model]]
- [[_COMMUNITY_Eligibility Result DTO|Eligibility Result DTO]]
- [[_COMMUNITY_Events Response DTO|Events Response DTO]]
- [[_COMMUNITY_Claim Events Model|Claim Events Model]]
- [[_COMMUNITY_Flight Response DTO|Flight Response DTO]]
- [[_COMMUNITY_Document Response DTO|Document Response DTO]]
- [[_COMMUNITY_Issue Response DTO|Issue Response DTO]]
- [[_COMMUNITY_EuContext Response DTO|EuContext Response DTO]]
- [[_COMMUNITY_Boarding Document Response DTO|Boarding Document Response DTO]]
- [[_COMMUNITY_Claim Response DTO|Claim Response DTO]]
- [[_COMMUNITY_Letter Response DTO|Letter Response DTO]]
- [[_COMMUNITY_Status Change Request DTO|Status Change Request DTO]]
- [[_COMMUNITY_EuContext Request DTO|EuContext Request DTO]]
- [[_COMMUNITY_Paid Claim Request DTO|Paid Claim Request DTO]]
- [[_COMMUNITY_Document Upload Request DTO|Document Upload Request DTO]]
- [[_COMMUNITY_Update Claim Details Request DTO|Update Claim Details Request DTO]]
- [[_COMMUNITY_Issue Request DTO|Issue Request DTO]]
- [[_COMMUNITY_Approve Claim Request DTO|Approve Claim Request DTO]]
- [[_COMMUNITY_Create Claim Request DTO|Create Claim Request DTO]]
- [[_COMMUNITY_Submit Claim Request DTO|Submit Claim Request DTO]]
- [[_COMMUNITY_Close Claim Request DTO|Close Claim Request DTO]]
- [[_COMMUNITY_Follow Up Request DTO|Follow Up Request DTO]]
- [[_COMMUNITY_Boarding Document Request DTO|Boarding Document Request DTO]]
- [[_COMMUNITY_Flight Request DTO|Flight Request DTO]]
- [[_COMMUNITY_Reject Claim Request DTO|Reject Claim Request DTO]]
- [[_COMMUNITY_Claim Repository|Claim Repository]]
- [[_COMMUNITY_Boarding Documents Model|Boarding Documents Model]]
- [[_COMMUNITY_Issue Model|Issue Model]]
- [[_COMMUNITY_Flight Model|Flight Model]]
- [[_COMMUNITY_EuContext Model|EuContext Model]]
- [[_COMMUNITY_App Bootstrap|App Bootstrap]]
- [[_COMMUNITY_DocumentTypes Enum|DocumentTypes Enum]]
- [[_COMMUNITY_IssueType Enum|IssueType Enum]]
- [[_COMMUNITY_EventTypes Enum|EventTypes Enum]]
- [[_COMMUNITY_ClaimStatus Enum|ClaimStatus Enum]]
- [[_COMMUNITY_Home Template|Home Template]]

## God Nodes (most connected - your core abstractions)
1. `ClaimIntegrationTest` - 35 edges
2. `ClaimLifecycleServiceImpl` - 17 edges
3. `ClaimServiceImplTest` - 16 edges
4. `ClaimController` - 14 edges
5. `ClaimService` - 14 edges
6. `ClaimLifecycleServiceImpl` - 12 edges
7. `EligibilityServiceImplTest` - 11 edges
8. `DocumentStorageServiceImpl` - 11 edges
9. `ClaimResponse` - 11 edges
10. `Claim` - 11 edges

## Surprising Connections (you probably didn't know these)
- `Claims MVP Progress Checklist` --references--> `DocumentStorageService`  [INFERRED]
  docs/PROGRESS_CHECKLIST.md → src/main/java/com/claims/mvp/claim/service/storage/DocumentStorageService.java
- `Claims MVP Progress Checklist` --references--> `EU Regulation 261/2004`  [INFERRED]
  docs/PROGRESS_CHECKLIST.md → src/main/java/com/claims/mvp/claim/service/letter/ClaimLetterServiceImpl.java
- `Claims MVP Progress Checklist` --references--> `Claim Status FSM (Finite State Machine)`  [INFERRED]
  docs/PROGRESS_CHECKLIST.md → src/main/java/com/claims/mvp/claim/service/workflow/ClaimWorkflowServiceImpl.java
- `Claims MVP Progress Checklist` --references--> `ClaimStatus`  [INFERRED]
  docs/PROGRESS_CHECKLIST.md → src/main/java/com/claims/mvp/claim/enums/ClaimStatus.java
- `Claims MVP Progress Checklist` --references--> `ClaimDocumentsService`  [INFERRED]
  docs/PROGRESS_CHECKLIST.md → src/main/java/com/claims/mvp/claim/service/documents/ClaimDocumentsService.java

## Hyperedges (group relationships)
- **User creation flow: Controller -> Service -> Mapper -> Repository -> Model** — usercontroller_controller, userservice_interface, userserviceimpl_service, usermapper_mapper, userrepository_dao, user_model, createuserrequest_dto, userresponse_dto [EXTRACTED 1.00]
- **Integration tests sharing Testcontainers PostgreSQL base** — integrationtestbase_test, claimsmvpapplicationtests_test, claimintegrationtest_test, postgresqlcontainer_infra [EXTRACTED 1.00]
- **Eligibility evaluation: Service evaluates Issue/Flight/EuContext -> returns EligibilityResult** — eligibilityservice_interface, eligibilityserviceimpl_service, eligibilityresult_dto [EXTRACTED 1.00]
- **Claim Lifecycle Request DTOs** — createclaimrequest_dto, submitclaimrequest_dto, approveclaimrequest_dto, paidclaimrequest_dto, statuschangerequest_dto [INFERRED 0.85]
- **ClaimResponse Composed DTOs** — claimresponse_dto, flightresponse_dto, issueresponse_dto, eucontextresponse_dto, boardingdocumentresponse_dto, userresponse_dto [EXTRACTED 1.00]
- **Exception Handling Infrastructure** — globalexceptionhandler_handler, errorresponse_record [EXTRACTED 1.00]
- **Claim Aggregate Root — Claim owns Flight, Issue, BoardingDocuments, and ClaimEvents via JPA cascade** — claim_model, flight_model, issue_model, boardingdocuments_model [INFERRED 0.95]
- **Claim lifecycle action DTOs all carry a single optional note field (close, follow-up, reject)** — closeclaimrequest_dto, followuprequest_dto, rejectclaimrequest_dto [INFERRED 0.85]
- **MapStruct mapper layer converts between domain models and DTOs/responses** — documentmapper_mapper, claimentitymapper_mapper, claimmapper_mapper [EXTRACTED 1.00]
- **Claim Lifecycle Orchestration: ClaimLifecycleServiceImpl coordinates ClaimWorkflowService, ClaimDocumentsService, EligibilityService, ClaimLetterService** — claimlifecycleserviceimpl_service, claimworkflowservice_interface, claimdocumentsservice_interface, eligibilityservice_dep, claimletterservice_interface [EXTRACTED 1.00]
- **Document Storage Validation: magic-bytes MIME check, size limit, allowed types before persisting BoardingDocuments** — documentstorageserviceimpl_service, magicbytes_validation, boardingdocuments_model, claimrepository_dep [EXTRACTED 1.00]
- **Pre-Submit Auto Transition: EligibilityService required docs + ClaimDocumentsService uploaded types -> ClaimWorkflowService.autoPreSubmitStatus** — claimlifecycleserviceimpl_service, eligibilityservice_dep, claimdocumentsservice_interface, claimworkflowservice_interface, claimstatus_enum [EXTRACTED 1.00]

## Communities

### Community 0 - "Claim Controller Layer"
Cohesion: 0.07
Nodes (6): ClaimControllerTest, ClaimDocumentsServiceImpl, ClaimEntityMapper, ClaimLetterServiceImpl, ClaimLifecycleServiceImpl, ClaimWorkflowServiceImpl

### Community 1 - "Document Storage"
Cohesion: 0.07
Nodes (40): BoardingDocuments, BoardingDocumentsRepository, BoardingDocumentsRepository, Claim, ClaimDocumentsService, ClaimDocumentsServiceImpl, ClaimEvents, ClaimLetterService (+32 more)

### Community 2 - "Integration Tests"
Cohesion: 0.23
Nodes (1): ClaimIntegrationTest

### Community 3 - "Claim Request DTOs"
Cohesion: 0.12
Nodes (27): ApproveClaimRequest, BoardingDocumentRequest, BoardingDocumentResponse, ClaimController, ClaimEntityMapper, ClaimResponse, ClaimService, CloseClaimRequest (+19 more)

### Community 4 - "Claim Service Tests"
Cohesion: 0.16
Nodes (2): ClaimServiceImplTest, EventsRepository

### Community 5 - "Test Infrastructure"
Cohesion: 0.18
Nodes (19): ClaimControllerTest, ClaimIntegrationTest, ClaimServiceImplTest, ClaimsMvpApplicationTests, CreateUserRequest, EligibilityResult, EligibilityService, EligibilityServiceImpl (+11 more)

### Community 6 - "Claim Repositories & Mappers"
Cohesion: 0.18
Nodes (3): BoardingDocumentsRepository, ClaimMapper, DocumentStorageServiceImpl

### Community 7 - "Eligibility Service Interface"
Cohesion: 0.3
Nodes (2): EligibilityService, EligibilityServiceImplTest

### Community 8 - "Claim Service Interface"
Cohesion: 0.13
Nodes (1): ClaimService

### Community 9 - "Claim Controller Interface"
Cohesion: 0.14
Nodes (1): ClaimController

### Community 10 - "User Domain"
Cohesion: 0.2
Nodes (3): UserControllerTest, UserRepository, UserServiceImpl

### Community 11 - "Document Storage Service"
Cohesion: 0.25
Nodes (1): DocumentStorageService

### Community 12 - "Global Exception Handler"
Cohesion: 0.33
Nodes (1): GlobalExceptionHandler

### Community 13 - "Document Controller"
Cohesion: 0.33
Nodes (1): DocumentController

### Community 14 - "Document Service"
Cohesion: 0.4
Nodes (1): ClaimDocumentsService

### Community 15 - "User Mapper"
Cohesion: 0.5
Nodes (1): UserMapper

### Community 16 - "Eligibility Engine"
Cohesion: 0.67
Nodes (1): EligibilityServiceImpl

### Community 17 - "Document Mapper"
Cohesion: 0.5
Nodes (1): DocumentMapper

### Community 18 - "Claim Workflow FSM"
Cohesion: 0.5
Nodes (1): ClaimWorkflowService

### Community 19 - "App Context Test"
Cohesion: 0.67
Nodes (1): ClaimsMvpApplicationTests

### Community 20 - "Integration Test Base"
Cohesion: 0.67
Nodes (1): IntegrationTestBase

### Community 21 - "Spring Boot Entry Point"
Cohesion: 0.67
Nodes (1): ClaimsMvpApplication

### Community 22 - "Home Controller"
Cohesion: 0.67
Nodes (1): HomeController

### Community 23 - "User Controller"
Cohesion: 0.67
Nodes (1): UserController

### Community 24 - "User Service Interface"
Cohesion: 0.67
Nodes (1): UserService

### Community 25 - "Claim Entity"
Cohesion: 0.67
Nodes (1): Claim

### Community 26 - "Letter Service Interface"
Cohesion: 0.67
Nodes (1): ClaimLetterService

### Community 27 - "User Response DTO"
Cohesion: 1.0
Nodes (1): UserResponse

### Community 28 - "Create User Request DTO"
Cohesion: 1.0
Nodes (1): CreateUserRequest

### Community 29 - "User Model"
Cohesion: 1.0
Nodes (1): User

### Community 30 - "Eligibility Result DTO"
Cohesion: 1.0
Nodes (1): EligibilityResult

### Community 31 - "Events Response DTO"
Cohesion: 1.0
Nodes (1): EventsResponse

### Community 32 - "Claim Events Model"
Cohesion: 1.0
Nodes (1): ClaimEvents

### Community 33 - "Flight Response DTO"
Cohesion: 1.0
Nodes (1): FlightResponse

### Community 34 - "Document Response DTO"
Cohesion: 1.0
Nodes (1): DocumentResponse

### Community 35 - "Issue Response DTO"
Cohesion: 1.0
Nodes (1): IssueResponse

### Community 36 - "EuContext Response DTO"
Cohesion: 1.0
Nodes (1): EuContextResponse

### Community 37 - "Boarding Document Response DTO"
Cohesion: 1.0
Nodes (1): BoardingDocumentResponse

### Community 38 - "Claim Response DTO"
Cohesion: 1.0
Nodes (1): ClaimResponse

### Community 39 - "Letter Response DTO"
Cohesion: 1.0
Nodes (1): LetterResponse

### Community 40 - "Status Change Request DTO"
Cohesion: 1.0
Nodes (1): StatusChangeRequest

### Community 41 - "EuContext Request DTO"
Cohesion: 1.0
Nodes (1): EuContextRequest

### Community 42 - "Paid Claim Request DTO"
Cohesion: 1.0
Nodes (1): PaidClaimRequest

### Community 43 - "Document Upload Request DTO"
Cohesion: 1.0
Nodes (1): DocumentUploadRequest

### Community 44 - "Update Claim Details Request DTO"
Cohesion: 1.0
Nodes (1): UpdateClaimDetailsRequest

### Community 45 - "Issue Request DTO"
Cohesion: 1.0
Nodes (1): IssueRequest

### Community 46 - "Approve Claim Request DTO"
Cohesion: 1.0
Nodes (1): ApproveClaimRequest

### Community 47 - "Create Claim Request DTO"
Cohesion: 1.0
Nodes (1): CreateClaimRequest

### Community 48 - "Submit Claim Request DTO"
Cohesion: 1.0
Nodes (1): SubmitClaimRequest

### Community 49 - "Close Claim Request DTO"
Cohesion: 1.0
Nodes (1): CloseClaimRequest

### Community 50 - "Follow Up Request DTO"
Cohesion: 1.0
Nodes (1): FollowUpRequest

### Community 51 - "Boarding Document Request DTO"
Cohesion: 1.0
Nodes (1): BoardingDocumentRequest

### Community 52 - "Flight Request DTO"
Cohesion: 1.0
Nodes (1): FlightRequest

### Community 53 - "Reject Claim Request DTO"
Cohesion: 1.0
Nodes (1): RejectClaimRequest

### Community 54 - "Claim Repository"
Cohesion: 1.0
Nodes (1): ClaimRepository

### Community 55 - "Boarding Documents Model"
Cohesion: 1.0
Nodes (1): BoardingDocuments

### Community 56 - "Issue Model"
Cohesion: 1.0
Nodes (1): Issue

### Community 57 - "Flight Model"
Cohesion: 1.0
Nodes (1): Flight

### Community 58 - "EuContext Model"
Cohesion: 1.0
Nodes (1): EuContext

### Community 59 - "App Bootstrap"
Cohesion: 1.0
Nodes (2): ClaimsMvpApplication, HomeController

### Community 60 - "DocumentTypes Enum"
Cohesion: 1.0
Nodes (0): 

### Community 61 - "IssueType Enum"
Cohesion: 1.0
Nodes (0): 

### Community 62 - "EventTypes Enum"
Cohesion: 1.0
Nodes (0): 

### Community 63 - "ClaimStatus Enum"
Cohesion: 1.0
Nodes (0): 

### Community 64 - "Home Template"
Cohesion: 1.0
Nodes (1): home.html Thymeleaf Template

## Knowledge Gaps
- **55 isolated node(s):** `UserResponse`, `CreateUserRequest`, `User`, `EligibilityResult`, `EventsResponse` (+50 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `User Response DTO`** (2 nodes): `UserResponse.java`, `UserResponse`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Create User Request DTO`** (2 nodes): `CreateUserRequest`, `CreateUserRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `User Model`** (2 nodes): `User.java`, `User`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Eligibility Result DTO`** (2 nodes): `EligibilityResult`, `EligibilityResult.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Events Response DTO`** (2 nodes): `EventsResponse`, `EventsResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Claim Events Model`** (2 nodes): `ClaimEvents`, `ClaimEvents.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight Response DTO`** (2 nodes): `FlightResponse`, `FlightResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Document Response DTO`** (2 nodes): `DocumentResponse`, `DocumentResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Issue Response DTO`** (2 nodes): `IssueResponse`, `IssueResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `EuContext Response DTO`** (2 nodes): `EuContextResponse`, `EuContextResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Boarding Document Response DTO`** (2 nodes): `BoardingDocumentResponse`, `BoardingDocumentResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Claim Response DTO`** (2 nodes): `ClaimResponse`, `ClaimResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Letter Response DTO`** (2 nodes): `LetterResponse`, `LetterResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Status Change Request DTO`** (2 nodes): `StatusChangeRequest.java`, `StatusChangeRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `EuContext Request DTO`** (2 nodes): `EuContextRequest`, `EuContextRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Paid Claim Request DTO`** (2 nodes): `PaidClaimRequest`, `PaidClaimRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Document Upload Request DTO`** (2 nodes): `DocumentUploadRequest`, `DocumentUploadRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Update Claim Details Request DTO`** (2 nodes): `UpdateClaimDetailsRequest.java`, `UpdateClaimDetailsRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Issue Request DTO`** (2 nodes): `IssueRequest`, `IssueRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Approve Claim Request DTO`** (2 nodes): `ApproveClaimRequest`, `ApproveClaimRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Create Claim Request DTO`** (2 nodes): `CreateClaimRequest`, `CreateClaimRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Submit Claim Request DTO`** (2 nodes): `SubmitClaimRequest.java`, `SubmitClaimRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Close Claim Request DTO`** (2 nodes): `CloseClaimRequest`, `CloseClaimRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Follow Up Request DTO`** (2 nodes): `FollowUpRequest`, `FollowUpRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Boarding Document Request DTO`** (2 nodes): `BoardingDocumentRequest`, `BoardingDocumentRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight Request DTO`** (2 nodes): `FlightRequest`, `FlightRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Reject Claim Request DTO`** (2 nodes): `RejectClaimRequest`, `RejectClaimRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Claim Repository`** (2 nodes): `ClaimRepository`, `ClaimRepository.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Boarding Documents Model`** (2 nodes): `BoardingDocuments`, `BoardingDocuments.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Issue Model`** (2 nodes): `Issue`, `Issue.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flight Model`** (2 nodes): `Flight`, `Flight.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `EuContext Model`** (2 nodes): `EuContext`, `EuContext.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Bootstrap`** (2 nodes): `ClaimsMvpApplication`, `HomeController`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `DocumentTypes Enum`** (1 nodes): `DocumentTypes.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `IssueType Enum`** (1 nodes): `IssueType.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `EventTypes Enum`** (1 nodes): `EventTypes.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `ClaimStatus Enum`** (1 nodes): `ClaimStatus.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Home Template`** (1 nodes): `home.html Thymeleaf Template`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ClaimLifecycleServiceImpl` connect `Claim Controller Layer` to `Claim Service Tests`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **Why does `ClaimController` connect `Claim Controller Interface` to `Claim Controller Layer`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Why does `ClaimResponse` connect `Claim Request DTOs` to `Document Storage`, `Test Infrastructure`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **What connects `UserResponse`, `CreateUserRequest`, `User` to the rest of the system?**
  _55 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Claim Controller Layer` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Document Storage` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Claim Request DTOs` be split into smaller, more focused modules?**
  _Cohesion score 0.12 - nodes in this community are weakly interconnected._