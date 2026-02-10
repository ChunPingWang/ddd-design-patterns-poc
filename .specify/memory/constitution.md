<!--
  Sync Impact Report
  ==================
  Version change: N/A → 1.0.0 (initial creation)

  Added principles:
    - I. Code Quality
    - II. Test-Driven Development (NON-NEGOTIABLE)
    - III. Behavior-Driven Development
    - IV. Domain-Driven Design
    - V. SOLID Principles
    - VI. Hexagonal Architecture & Layer Boundaries

  Added sections:
    - Architecture Constraints (detailed hexagonal layer rules)
    - Development Workflow (TDD/BDD cycle, quality gates)
    - Governance

  Removed sections: N/A (initial creation)

  Templates requiring updates:
    - .specify/templates/plan-template.md ✅ compatible (Constitution Check
      section will be populated per these principles)
    - .specify/templates/spec-template.md ✅ compatible (BDD Given/When/Then
      format already present in acceptance scenarios)
    - .specify/templates/tasks-template.md ✅ compatible (test-first workflow
      and phase structure align with TDD mandate)
    - .specify/templates/checklist-template.md ✅ compatible (generic template)
    - .specify/templates/agent-file-template.md ✅ compatible (generic template)

  Follow-up TODOs: None
-->

# DDD Design Patterns PoC Constitution

## Core Principles

### I. Code Quality

- All code MUST follow consistent naming conventions aligned with the
  Ubiquitous Language of the bounded context.
- Methods MUST be short and focused on a single responsibility; cyclomatic
  complexity per method MUST NOT exceed 10.
- No dead code, commented-out code, or TODO comments are permitted in
  production branches.
- All public APIs MUST have clear, self-documenting signatures; comments
  are reserved for explaining "why", not "what".
- Static analysis and linting MUST be enforced in the build pipeline;
  code that fails lint checks MUST NOT be merged.

### II. Test-Driven Development (NON-NEGOTIABLE)

- TDD is mandatory: tests MUST be written before implementation code.
- The Red-Green-Refactor cycle MUST be strictly followed:
  1. Write a failing test that defines the desired behavior.
  2. Write the minimum code to make the test pass.
  3. Refactor while keeping all tests green.
- Unit tests MUST cover domain logic and application services in
  isolation from infrastructure concerns.
- Integration tests MUST verify adapter behavior against real or
  containerized dependencies (e.g., database, message broker).
- Test coverage for domain and application layers MUST be >= 80%.
- Tests MUST be deterministic, repeatable, and independent of
  execution order.

### III. Behavior-Driven Development

- Every user story MUST include acceptance scenarios written in
  Given/When/Then format before implementation begins.
- Acceptance scenarios MUST be expressed in the Ubiquitous Language of
  the domain, not in technical implementation terms.
- Scenarios MUST be independently verifiable: each scenario defines a
  single observable behavior.
- Acceptance tests MUST serve as living documentation; they MUST be
  kept in sync with the codebase at all times.
- Edge cases and error scenarios MUST be captured as explicit
  Given/When/Then scenarios, not left implicit.

### IV. Domain-Driven Design

- The domain layer is the core of the system and MUST NOT depend on any
  framework, library, or infrastructure technology.
- A Ubiquitous Language MUST be established per bounded context and
  used consistently in code, tests, documentation, and conversations.
- Domain concepts MUST be modeled using DDD building blocks:
  - **Entities**: Objects with identity and lifecycle.
  - **Value Objects**: Immutable objects defined by their attributes.
  - **Aggregates**: Consistency boundaries with a single root entity.
  - **Domain Events**: Represent facts that have occurred in the domain.
  - **Domain Services**: Stateless operations that do not belong to a
    single entity or value object.
  - **Repositories (interface only)**: Defined in the domain layer as
    interfaces; implementations reside in the infrastructure layer.
- Aggregate boundaries MUST be respected: cross-aggregate references
  MUST use identifiers, not direct object references.
- Domain invariants MUST be enforced within the aggregate root; no
  external code may bypass aggregate validation.

### V. SOLID Principles

- **Single Responsibility (SRP)**: Every class and module MUST have one
  and only one reason to change. If a class handles both domain logic
  and persistence, it violates SRP.
- **Open/Closed (OCP)**: Modules MUST be open for extension but closed
  for modification. New behavior MUST be added via new implementations
  of existing abstractions, not by modifying existing code.
- **Liskov Substitution (LSP)**: Subtypes MUST be substitutable for
  their base types without altering the correctness of the program.
  Contract tests MUST verify substitutability.
- **Interface Segregation (ISP)**: Clients MUST NOT be forced to depend
  on interfaces they do not use. Ports MUST be fine-grained and
  role-specific (e.g., separate `OrderReader` and `OrderWriter` instead
  of a monolithic `OrderRepository`).
- **Dependency Inversion (DIP)**: High-level modules (domain,
  application) MUST NOT depend on low-level modules (infrastructure).
  Both MUST depend on abstractions (interfaces/ports). This is
  enforced by the hexagonal architecture layer rules below.

### VI. Hexagonal Architecture & Layer Boundaries

- The system MUST be structured in three concentric layers:
  - **Domain Layer (innermost)**: Pure business logic, entities, value
    objects, aggregates, domain events, domain services, and repository
    interfaces (ports).
  - **Application Layer (middle)**: Use cases / application services
    that orchestrate domain objects. Defines inbound ports (use case
    interfaces) and references outbound ports (repository/service
    interfaces from domain).
  - **Infrastructure Layer (outermost)**: All framework code, database
    adapters, messaging adapters, REST/gRPC controllers, configuration,
    and third-party library integrations.

- **Framework Isolation**: Any framework or third-party library MUST
  only be used in the infrastructure layer. The domain and application
  layers MUST remain framework-free and technology-agnostic.

- **Dependency Direction**: Dependencies MUST flow inward only:
  - Infrastructure → Application → Domain (allowed).
  - Domain → Application or Infrastructure (FORBIDDEN).
  - Application → Infrastructure (FORBIDDEN without interface).
  - The infrastructure layer MAY directly reference application layer
    and domain layer types.
  - The application and domain layers MUST access infrastructure
    capabilities exclusively through interfaces (ports) defined in
    the domain or application layer.

- **Mapper-Based Data Transfer**: Data crossing layer boundaries MUST
  be transformed via dedicated mapper classes:
  - Infrastructure ↔ Application: Mappers MUST convert between
    infrastructure DTOs (e.g., JPA entities, API request/response
    objects) and domain/application objects.
  - No domain object may be directly exposed to or consumed from
    external interfaces (REST, messaging, persistence).
  - Each mapper MUST be a single-purpose class or method with no
    business logic; mapping is purely structural transformation.

- **Port & Adapter Naming Convention**:
  - Inbound ports: Use case interfaces (e.g., `PlaceOrderUseCase`).
  - Outbound ports: Repository/service interfaces defined in domain
    (e.g., `OrderRepository`, `PaymentGateway`).
  - Adapters: Infrastructure implementations of ports (e.g.,
    `JpaOrderRepository`, `StripePaymentGateway`).

## Architecture Constraints

- **Package/Module Structure** MUST reflect the hexagonal layers:
  ```
  <bounded-context>/
  ├── domain/           # Entities, VOs, Aggregates, Domain Events,
  │                     # Domain Services, Repository Interfaces (Ports)
  ├── application/      # Use Cases, Application Services,
  │                     # Inbound Port Interfaces, DTOs (internal)
  └── infrastructure/   # Adapters, Controllers, Persistence,
                        # Messaging, Configuration, Mappers, Framework code
  ```
- Circular dependencies between layers are FORBIDDEN. Build tooling
  MUST enforce layer dependency rules (e.g., ArchUnit for Java,
  dependency-cruiser for TypeScript).
- The domain layer MUST compile and pass all unit tests without any
  infrastructure or framework dependency on the classpath/module path.
- Shared kernel between bounded contexts, if needed, MUST be extracted
  into a separate module with its own versioning and explicit contract.

## Development Workflow

- **TDD/BDD Cycle for Every Feature**:
  1. Write BDD acceptance scenarios (Given/When/Then) from user stories.
  2. Write failing unit tests for domain logic (Red).
  3. Implement domain logic to pass tests (Green).
  4. Refactor domain code while tests remain green.
  5. Write failing integration tests for adapters.
  6. Implement adapters to pass integration tests.
  7. Verify acceptance scenarios end-to-end.

- **Code Review Quality Gates**:
  - All tests MUST pass before merge.
  - Layer dependency rules MUST be verified (no inward violations).
  - Mapper coverage MUST be verified: no domain objects leak to or
    from infrastructure boundaries.
  - Naming MUST align with the Ubiquitous Language.
  - SOLID principle compliance MUST be reviewed.

- **Continuous Integration**:
  - Build pipeline MUST run unit tests, integration tests, static
    analysis, and layer dependency checks on every commit.
  - Acceptance tests MUST run before release.

## Governance

- This constitution supersedes all other development practices for this
  project. In case of conflict, this document takes precedence.
- Amendments MUST be documented with a rationale, approved by the team,
  and accompanied by a migration plan for existing code if the change
  is backward-incompatible.
- All pull requests and code reviews MUST verify compliance with the
  principles defined herein. Non-compliance MUST be flagged and
  resolved before merge.
- Complexity beyond what this constitution prescribes MUST be justified
  in writing (e.g., in a plan.md Complexity Tracking table) and
  approved before implementation.
- Version policy: MAJOR for principle removal/redefinition, MINOR for
  new principles or material expansion, PATCH for clarifications and
  wording fixes.

**Version**: 1.0.0 | **Ratified**: 2026-02-11 | **Last Amended**: 2026-02-11
