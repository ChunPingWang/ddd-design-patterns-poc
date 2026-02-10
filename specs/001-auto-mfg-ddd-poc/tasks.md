# Tasks: AutoMFG — Automotive Manufacturing Order-to-Delivery DDD PoC

**Input**: Design documents from `/specs/001-auto-mfg-ddd-poc/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD is NON-NEGOTIABLE per constitution. Tests MUST be written before implementation.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to

---

## Phase 1: Setup (Project Infrastructure)

**Purpose**: Maven multi-module project initialization and shared kernel

- [X] T001 Create Maven parent POM with dependency management (Java 21, Spring Boot 3.3.x, JUnit 5, AssertJ, ArchUnit) at `auto-mfg/pom.xml`
- [X] T002 Create shared-kernel module with DomainEvent base class, common value objects (Money), and event publishing port at `auto-mfg/shared-kernel/`
- [X] T003 [P] Create order-context module structure (order-domain, order-application, order-infrastructure POMs) at `auto-mfg/order-context/`
- [X] T004 [P] Create manufacturing-context module structure (manufacturing-domain, manufacturing-application, manufacturing-infrastructure POMs) at `auto-mfg/manufacturing-context/`
- [X] T005 [P] Create vehicle-config-context module structure (vehicle-config-domain, vehicle-config-application, vehicle-config-infrastructure POMs) at `auto-mfg/vehicle-config-context/`
- [X] T006 [P] Create material-context mock module at `auto-mfg/material-context/material-mock/`
- [X] T007 Create bootstrap module with Spring Boot main class, application.yml, and Docker Compose file at `auto-mfg/bootstrap/`
- [X] T008 Create .gitignore for Java/Maven/Spring Boot project

**Checkpoint**: `mvn clean compile` succeeds across all modules

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T009 Implement DomainEvent base class (sealed interface) and domain event publisher port in shared-kernel domain at `auto-mfg/shared-kernel/src/main/java/com/automfg/shared/domain/`
- [X] T010 [P] Implement common Value Objects: VIN, ProductionOrderNumber, MaterialBatchId, Duration, WorkStationId in manufacturing-domain at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/`
- [X] T011 [P] Implement VehicleConfiguration aggregate root, ModelSpec, OptionItem, CompatibilityRule, InspectionChecklist in vehicle-config-domain at `auto-mfg/vehicle-config-context/vehicle-config-domain/src/main/java/com/automfg/vehicleconfig/domain/model/`
- [X] T012 [P] Create Flyway migration scripts for all schemas (order_ctx, mfg_ctx, vehicle_ctx, infrastructure tables) at `auto-mfg/bootstrap/src/main/resources/db/migration/`
- [X] T013 [P] Create seed data SQL for vehicle models, colors, options, compatibility rules, inspection checklists, workstations, dealers at `auto-mfg/bootstrap/src/main/resources/seed/`
- [X] T014 [P] Setup ArchUnit architecture tests enforcing hexagonal layer rules at `auto-mfg/bootstrap/src/test/java/com/automfg/architecture/ArchitectureTest.java`
- [X] T015 [P] Create ProcessedEvent and DomainEventOutbox infrastructure tables and JPA entities at `auto-mfg/shared-kernel/`
- [X] T016 [P] Implement MockMaterialAvailabilityAdapter in material-mock module at `auto-mfg/material-context/material-mock/`
- [X] T017 Configure Spring application context, datasource, and Kafka properties in bootstrap at `auto-mfg/bootstrap/src/main/resources/application.yml`

**Checkpoint**: `mvn clean verify` succeeds; ArchUnit tests pass; seed data loads

---

## Phase 3: User Story 1 — Dealer Places a Vehicle Order (Priority: P1) 🎯 MVP

**Goal**: Dealer selects model/color/options, system validates compatibility, calculates delivery date & price, creates order, publishes OrderPlacedEvent

**Independent Test**: Submit orders with various configurations; verify BR-01 (50-order limit), BR-02 (compatibility), BR-03 (45-day delivery), price calculation, event publishing

### Tests for User Story 1

- [X] T018 [P] [US1] Unit tests for Order aggregate (place order, validate options, enforce limits, calculate delivery date) at `auto-mfg/order-context/order-domain/src/test/java/com/automfg/order/domain/model/OrderTest.java`
- [X] T019 [P] [US1] Unit tests for VehicleConfiguration compatibility validation at `auto-mfg/vehicle-config-context/vehicle-config-domain/src/test/java/com/automfg/vehicleconfig/domain/model/VehicleConfigurationTest.java`

### Implementation for User Story 1

- [X] T020 [P] [US1] Implement Order aggregate root with status, change count, delivery date calculation, price quote, and domain events at `auto-mfg/order-context/order-domain/src/main/java/com/automfg/order/domain/model/Order.java`
- [X] T021 [P] [US1] Implement OrderRepository port and VehicleConfigGateway port (ACL interface) at `auto-mfg/order-context/order-domain/src/main/java/com/automfg/order/domain/port/`
- [X] T022 [P] [US1] Implement OrderPlacedEvent and OrderChangedEvent domain events at `auto-mfg/order-context/order-domain/src/main/java/com/automfg/order/domain/event/`
- [X] T023 [US1] Implement PlaceOrderUseCase (application service) at `auto-mfg/order-context/order-application/src/main/java/com/automfg/order/application/usecase/PlaceOrderUseCaseImpl.java`
- [X] T024 [US1] Implement JPA Order entity, mapper, and JpaOrderRepository adapter at `auto-mfg/order-context/order-infrastructure/src/main/java/com/automfg/order/infrastructure/`
- [X] T025 [US1] Implement VehicleConfigACLAdapter (anti-corruption layer) at `auto-mfg/order-context/order-infrastructure/src/main/java/com/automfg/order/infrastructure/adapter/outbound/VehicleConfigACLAdapter.java`
- [X] T026 [US1] Implement OrderController REST endpoint (POST /api/v1/orders, GET /api/v1/orders) at `auto-mfg/order-context/order-infrastructure/src/main/java/com/automfg/order/infrastructure/adapter/inbound/OrderController.java`
- [X] T027 [US1] Implement VehicleConfiguration JPA repository and seed data loading at `auto-mfg/vehicle-config-context/vehicle-config-infrastructure/`
- [X] T028 [US1] Integration test: place order end-to-end with compatibility check and event publishing at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/PlaceOrderE2ETest.java`

**Checkpoint**: POST /api/v1/orders creates order, validates options, publishes event; `mvn verify` passes

---

## Phase 4: User Story 2 — System Creates Production Order (Priority: P1)

**Goal**: OrderPlacedEvent triggers production order creation with VIN assignment, BOM expansion, material check, idempotent consumer

**Independent Test**: Publish OrderPlacedEvent; verify production order created with VIN, BOM, correct status; verify duplicate event handling

### Tests for User Story 2

- [X] T029 [P] [US2] Unit tests for ProductionOrder aggregate (create with material available/pending, VIN assignment, BOM snapshot) at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/model/ProductionOrderTest.java`
- [X] T030 [P] [US2] Unit tests for BomExpansionService domain service at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/service/BomExpansionServiceTest.java`

### Implementation for User Story 2

- [X] T031 [US2] Implement ProductionOrder aggregate root with factory method, status transitions, BomSnapshot, VIN at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/ProductionOrder.java`
- [X] T032 [P] [US2] Implement BomSnapshot, BomLineItem value objects at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/`
- [X] T033 [P] [US2] Implement manufacturing domain events (ProductionOrderScheduledEvent, MaterialShortageEvent) at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/event/`
- [X] T034 [P] [US2] Implement ProductionOrderRepository port and MaterialAvailabilityGateway port at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/port/`
- [X] T035 [US2] Implement BomExpansionService domain service at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/service/BomExpansionService.java`
- [X] T036 [US2] Implement CreateProductionOrderUseCase application service with idempotency check at `auto-mfg/manufacturing-context/manufacturing-application/src/main/java/com/automfg/manufacturing/application/usecase/CreateProductionOrderUseCaseImpl.java`
- [X] T037 [US2] Implement JPA ProductionOrder entity, mapper, and JpaProductionOrderRepository adapter at `auto-mfg/manufacturing-context/manufacturing-infrastructure/`
- [X] T038 [US2] Implement OrderEventConsumer (Kafka/Spring event listener with ACL) and idempotent consumer check at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/adapter/inbound/OrderEventConsumer.java`
- [X] T039 [US2] Implement ProductionOrderController (GET endpoints) at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/adapter/inbound/ProductionOrderController.java`
- [X] T040 [US2] Integration test: OrderPlacedEvent → production order creation + duplicate event handling at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/CreateProductionOrderE2ETest.java`

**Checkpoint**: OrderPlacedEvent creates production order with VIN and BOM; duplicate events ignored; `mvn verify` passes

---

## Phase 5: User Story 3 — Production Line Operator Advances Assembly (Priority: P1)

**Goal**: Operator scans at workstation, system shows tasks, operator completes with batch numbers, sequential station enforcement, overtime alerts

**Independent Test**: Simulate operator scanning through stations; verify sequence enforcement, batch recording, status transitions

### Tests for User Story 3

- [X] T041 [P] [US3] Unit tests for AssemblyProcess and AssemblyStep (station sequence, batch required, overtime alert, status transitions) at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/model/AssemblyProcessTest.java`
- [X] T042 [P] [US3] Unit tests for ProductionOrder.startProduction() and completeAssemblyStep() at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/model/ProductionOrderAssemblyTest.java`

### Implementation for User Story 3

- [X] T043 [US3] Implement AssemblyProcess entity and AssemblyStep entity with station sequencing, batch recording, overtime detection at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/`
- [X] T044 [US3] Implement ProductionOrder.startProduction() and completeAssemblyStep() methods with domain events (ProductionStartedEvent, AssemblyCompletedEvent, AssemblyOvertimeAlertEvent) at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/ProductionOrder.java`
- [X] T045 [US3] Implement StartProductionUseCase and CompleteAssemblyStepUseCase application services at `auto-mfg/manufacturing-context/manufacturing-application/src/main/java/com/automfg/manufacturing/application/usecase/`
- [X] T046 [US3] Implement REST endpoints: POST /start, POST /assembly-steps/{id}/complete, GET /assembly-steps at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/adapter/inbound/ProductionOrderController.java`
- [X] T047 [US3] Implement JPA entities for AssemblyProcess, AssemblyStep with immutable completed records at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/persistence/`
- [X] T048 [US3] Integration test: full assembly flow from start to ASSEMBLY_COMPLETED at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/AssemblyFlowE2ETest.java`

**Checkpoint**: Operator can scan, complete tasks with batch numbers, stations advance sequentially; `mvn verify` passes

---

## Phase 6: User Story 4 — Quality Inspection (Priority: P2)

**Goal**: Inspector loads checklist, records item results, safety-fail logic, four-eyes review, rework order creation, re-inspection after rework

**Independent Test**: Run inspection with various pass/fail combinations; verify safety logic, conditional pass limits, four-eyes enforcement

### Tests for User Story 4

- [X] T049 [P] [US4] Unit tests for QualityInspection aggregate (safety fail, conditional pass limit, four-eyes, immutable results) at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/model/QualityInspectionTest.java`
- [X] T050 [P] [US4] Unit tests for InspectionCompletionService domain service at `auto-mfg/manufacturing-context/manufacturing-domain/src/test/java/com/automfg/manufacturing/domain/service/InspectionCompletionServiceTest.java`

### Implementation for User Story 4

- [X] T051 [US4] Implement QualityInspection aggregate root with InspectionItem, safety/conditional logic, four-eyes review at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/QualityInspection.java`
- [X] T052 [P] [US4] Implement InspectionItem entity and InspectionResult enum at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/`
- [X] T053 [US4] Implement InspectionCompletionService domain service (cross-aggregate: QualityInspection + ProductionOrder) at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/service/InspectionCompletionService.java`
- [X] T054 [US4] Implement ReworkOrder entity and rework completion → re-inspection flow at `auto-mfg/manufacturing-context/manufacturing-domain/src/main/java/com/automfg/manufacturing/domain/model/ReworkOrder.java`
- [X] T055 [US4] Implement CompleteInspectionUseCase and ReviewInspectionUseCase application services at `auto-mfg/manufacturing-context/manufacturing-application/src/main/java/com/automfg/manufacturing/application/usecase/`
- [X] T056 [US4] Implement QualityInspection JPA entities (immutable inspection results) and mapper at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/persistence/`
- [X] T057 [US4] Implement InspectionController REST endpoints at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/adapter/inbound/InspectionController.java`
- [X] T058 [US4] Integration test: full inspection flow including rework and re-inspection at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/QualityInspectionE2ETest.java`

**Checkpoint**: Inspection pass/fail/rework cycle works; four-eyes enforced; VehicleCompletedEvent published on pass; `mvn verify` passes

---

## Phase 7: User Story 5 — Dealer Changes Order (Priority: P2)

**Goal**: Dealer changes color/options on pre-production orders; model change = cancel + new; max 3 changes; production-started rejection

**Independent Test**: Submit changes against orders in various statuses; verify acceptance/rejection logic

### Tests for User Story 5

- [X] T059 [P] [US5] Unit tests for Order.changeConfiguration() (max changes, production status check, model change = cancel) at `auto-mfg/order-context/order-domain/src/test/java/com/automfg/order/domain/model/OrderChangeTest.java`

### Implementation for User Story 5

- [X] T060 [US5] Implement Order.changeConfiguration() and Order.cancel() domain methods with OrderChangedEvent at `auto-mfg/order-context/order-domain/src/main/java/com/automfg/order/domain/model/Order.java`
- [X] T061 [US5] Implement ChangeOrderUseCase application service (coordinates with manufacturing context for modifiability check) at `auto-mfg/order-context/order-application/src/main/java/com/automfg/order/application/usecase/ChangeOrderUseCaseImpl.java`
- [X] T062 [US5] Implement POST /api/v1/orders/{orderId}/changes endpoint at `auto-mfg/order-context/order-infrastructure/src/main/java/com/automfg/order/infrastructure/adapter/inbound/OrderController.java`
- [X] T063 [US5] Implement OrderChangedEvent consumer in manufacturing context (update BOM if modifiable, reject if not) at `auto-mfg/manufacturing-context/manufacturing-infrastructure/src/main/java/com/automfg/manufacturing/infrastructure/adapter/inbound/OrderEventConsumer.java`
- [X] T064 [US5] Integration test: order change flow including rejection when in production at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/ChangeOrderE2ETest.java`

**Checkpoint**: Order changes accepted/rejected correctly; model change creates new order; `mvn verify` passes

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and cleanup

- [X] T065 [P] Run full ArchUnit test suite and fix any violations at `auto-mfg/bootstrap/src/test/java/com/automfg/architecture/`
- [X] T066 [P] Add SpringDoc OpenAPI annotations to all controllers for Swagger UI at all infrastructure adapter/inbound classes
- [X] T067 Run full E2E test: order → production → assembly → inspection → completed event flow at `auto-mfg/bootstrap/src/test/java/com/automfg/e2e/FullFlowE2ETest.java`
- [X] T068 Verify domain layer test coverage >= 90% and generate coverage report
- [X] T069 Final Docker Compose validation: `docker compose up` + `mvn verify`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user stories
- **Phase 3 (US1 Order Placement)**: Depends on Phase 2
- **Phase 4 (US2 Production Order)**: Depends on Phase 2; integrates with US1 events
- **Phase 5 (US3 Assembly)**: Depends on Phase 4 (needs ProductionOrder)
- **Phase 6 (US4 Quality Inspection)**: Depends on Phase 5 (needs ASSEMBLY_COMPLETED)
- **Phase 7 (US5 Order Change)**: Depends on Phase 3 (needs Order) + Phase 4 (needs ProductionOrder)
- **Phase 8 (Polish)**: Depends on all prior phases

### Parallel Opportunities

- T003, T004, T005, T006 (module creation) can run in parallel
- T010, T011, T012, T013, T014, T015, T016 (foundational) can run in parallel
- US1 tests (T018, T019) can run in parallel
- US3 and US5 can potentially run in parallel after their dependencies are met

---

## Notes

- TDD is mandatory per constitution: write failing tests FIRST
- Domain modules must have zero Spring dependencies
- All assembly steps and inspection results are immutable after recording
- Event consumers must be idempotent (ProcessedEvent table)
- Commit and push with zh-TW messages after each phase
