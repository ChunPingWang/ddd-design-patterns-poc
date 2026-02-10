# Implementation Plan: AutoMFG — Automotive Manufacturing Order-to-Delivery PoC

**Branch**: `001-auto-mfg-ddd-poc` | **Date**: 2026-02-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-auto-mfg-ddd-poc/spec.md`

## Summary

Build a DDD tactical design PoC for an automotive manufacturing order-to-delivery system using Java 21, Spring Boot 3, and Hexagonal Architecture. The system covers 4 bounded contexts (Order Management, Manufacturing Management, Vehicle Configuration, Material Management) with 5 core use cases demonstrating aggregates, domain events, value objects, and cross-context communication. The Manufacturing context is the core domain with full implementation; Material Management is mocked; Logistics is stubbed.

## Technical Context

**Language/Version**: Java 21 (LTS)
**Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Kafka, SpringDoc OpenAPI
**Build**: Maven 3.9.x Multi-Module
**Storage**: PostgreSQL (via Testcontainers for tests; H2 for local dev)
**Messaging**: Apache Kafka (Embedded Kafka for tests; Docker Compose for local dev)
**Testing**: JUnit 5 + AssertJ + ArchUnit + Testcontainers
**Target Platform**: Linux server / Docker containers
**Project Type**: Multi-module Maven backend application (REST API)
**Performance Goals**: Order placement < 2s; workstation scan < 500ms
**Constraints**: Domain layer zero-dependency on frameworks; 15-year immutable audit trail for quality records
**Scale/Scope**: PoC — single factory, seed data, no auth, ~10 entities across 4 contexts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Code Quality | PASS | Ubiquitous Language from PRD glossary enforced in all naming; cyclomatic complexity limit enforced via build plugins |
| II. TDD (NON-NEGOTIABLE) | PASS | All tasks will follow Red-Green-Refactor; domain tests written before implementation; ≥80% coverage target (spec requires ≥90% for domain) |
| III. BDD | PASS | All 5 user stories have Given/When/Then acceptance scenarios; edge cases have explicit scenarios |
| IV. DDD | PASS | Aggregates (ProductionOrder, QualityInspection, Order, VehicleConfiguration), Value Objects (VIN, ProductionOrderNumber, MaterialBatchId, Duration, WorkStationId), Domain Events, Repository Ports — all in pure domain layer |
| V. SOLID | PASS | SRP per aggregate; OCP via InspectionPolicy interface; LSP via InMemory/Jpa repo substitution; ISP via Reader/Writer port split; DIP via ports in domain, adapters in infrastructure |
| VI. Hexagonal Architecture | PASS | 4-module Maven structure per context: domain (pure Java) → application → infrastructure → bootstrap; ArchUnit tests guard layer rules |

**Gate Result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-auto-mfg-ddd-poc/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
│   ├── order-api.yaml
│   ├── manufacturing-api.yaml
│   └── quality-api.yaml
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
auto-mfg/
├── pom.xml                                    # Parent POM (dependency management)
│
├── shared-kernel/                             # Shared types across contexts
│   └── src/main/java/
│       └── com/automfg/shared/
│           ├── domain/                        # DomainEvent base, common VOs
│           └── infrastructure/                # Event publishing infrastructure
│
├── order-context/
│   ├── order-domain/
│   │   └── src/main/java/
│   │       └── com/automfg/order/domain/
│   │           ├── model/                     # Order (Aggregate Root), OrderItem
│   │           ├── event/                     # OrderPlacedEvent, OrderChangedEvent
│   │           ├── service/                   # DeliveryDateCalculator, PriceCalculator
│   │           └── port/                      # OrderRepository, VehicleConfigGateway
│   ├── order-application/
│   │   └── src/main/java/
│   │       └── com/automfg/order/application/
│   │           ├── usecase/                   # PlaceOrderUseCase, ChangeOrderUseCase
│   │           ├── port/                      # Input port interfaces
│   │           └── dto/                       # Command & Query DTOs
│   └── order-infrastructure/
│       └── src/main/java/
│           └── com/automfg/order/infrastructure/
│               ├── adapter/inbound/           # REST controllers
│               ├── adapter/outbound/          # JPA repos, event publishers
│               ├── persistence/              # JPA entities, mappers
│               └── config/                   # Spring config
│
├── manufacturing-context/
│   ├── manufacturing-domain/
│   │   └── src/main/java/
│   │       └── com/automfg/manufacturing/domain/
│   │           ├── model/                     # ProductionOrder, AssemblyProcess, AssemblyStep, BomSnapshot
│   │           ├── event/                     # ProductionStartedEvent, AssemblyCompletedEvent, etc.
│   │           ├── service/                   # InspectionCompletionService, BomExpansionService
│   │           └── port/                      # ProductionOrderRepository, MaterialAvailabilityGateway
│   ├── manufacturing-application/
│   │   └── src/main/java/
│   │       └── com/automfg/manufacturing/application/
│   │           ├── usecase/                   # CreateProductionOrderUseCase, CompleteAssemblyStepUseCase, etc.
│   │           ├── port/                      # Input port interfaces
│   │           └── dto/                       # Commands & Queries
│   └── manufacturing-infrastructure/
│       └── src/main/java/
│           └── com/automfg/manufacturing/infrastructure/
│               ├── adapter/inbound/           # REST controllers, Kafka consumers (ACL)
│               ├── adapter/outbound/          # JPA repos, Kafka publishers
│               ├── persistence/              # JPA entities, mappers
│               └── config/                   # Spring config
│
├── vehicle-config-context/
│   ├── vehicle-config-domain/
│   │   └── src/main/java/
│   │       └── com/automfg/vehicleconfig/domain/
│   │           ├── model/                     # VehicleConfiguration, ModelSpec, OptionItem
│   │           └── port/                      # VehicleConfigRepository
│   ├── vehicle-config-application/
│   └── vehicle-config-infrastructure/
│
├── material-context/                          # Mocked for PoC
│   └── material-mock/
│       └── src/main/java/
│           └── com/automfg/material/mock/     # MockMaterialAvailabilityAdapter
│
└── bootstrap/                                 # Spring Boot main application
    ├── src/main/java/
    │   └── com/automfg/AutoMfgApplication.java
    ├── src/main/resources/
    │   ├── application.yml
    │   ├── db/migration/                      # Flyway migrations
    │   └── seed/                              # Seed data (vehicle models, options, checklists)
    └── src/test/java/
        └── com/automfg/
            ├── architecture/                  # ArchUnit tests
            └── e2e/                           # End-to-end scenario tests
```

**Structure Decision**: Multi-module Maven project with 4 bounded contexts. Each context (except mocked Material) follows the 3-layer hexagonal split: domain / application / infrastructure. A shared-kernel module provides base types (DomainEvent, common Value Objects). A single bootstrap module wires everything together as one deployable Spring Boot application.

## Complexity Tracking

No constitution violations to justify. The multi-module Maven structure is a direct requirement of the hexagonal architecture principle (Constitution VI) and DDD bounded context separation (Constitution IV). Repository interfaces in the domain layer are mandated by DIP (Constitution V).

## Constitution Re-Check (Post Phase 1 Design)

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Ubiquitous Language maintained in data model and API contracts |
| II. TDD | PASS | quickstart.md includes TDD workflow; test structure in project layout |
| III. BDD | PASS | Acceptance scenarios from spec map directly to E2E tests |
| IV. DDD | PASS | All tactical patterns represented: Aggregates, VOs, Events, Ports, Domain Services |
| V. SOLID | PASS | ISP in port interfaces; DIP in all repository/gateway patterns |
| VI. Hexagonal | PASS | Strict 3-layer separation enforced per module; ArchUnit tests designed |

**Post-Design Gate Result**: ALL PASS.
