# Research: AutoMFG DDD PoC

**Feature**: 001-auto-mfg-ddd-poc
**Date**: 2026-02-11

## Research Tasks & Findings

### R-01: DDD Tactical Patterns with Java 21 + Spring Boot 3

**Decision**: Use Java 21 records for Value Objects, sealed interfaces for domain events, and plain Java classes for Entities/Aggregates. Domain modules have zero Spring dependencies.

**Rationale**: Java 21 records provide immutability and value-based equality by default — perfect for DDD Value Objects. Sealed interfaces enable exhaustive pattern matching in event handlers (Java 21 switch expressions). Spring Boot 3's Jakarta namespace (jakarta.persistence) is only used in infrastructure modules.

**Alternatives considered**:
- Lombok for boilerplate reduction → Rejected: records already eliminate boilerplate for VOs; Lombok in domain layer adds an annotation processor dependency.
- Kotlin data classes → Rejected: project mandates Java 21; records serve the same purpose.

### R-02: Event-Driven Cross-Context Communication with At-Least-Once Delivery

**Decision**: Use Spring Application Events for intra-process domain events within a single bounded context. Use Apache Kafka with Transactional Outbox pattern for cross-context events requiring at-least-once delivery.

**Rationale**: For the PoC (single deployable), Spring Application Events give synchronous in-process delivery for local domain events. For cross-context events (OrderPlacedEvent → Manufacturing, VehicleCompletedEvent → Logistics stub), Kafka provides durable at-least-once delivery. The Transactional Outbox pattern (domain events saved to an outbox table in the same transaction as the aggregate, then relayed to Kafka) prevents event loss without requiring distributed transactions.

**Alternatives considered**:
- Pure Kafka for all events → Rejected for PoC: adds infrastructure overhead for events that don't need to cross context boundaries.
- Spring Integration → Rejected: heavier abstraction than needed for this PoC scope.
- RabbitMQ → Rejected: Kafka's log-based storage better supports replay for at-least-once delivery patterns.

### R-03: Idempotent Consumer Pattern

**Decision**: Use event ID-based deduplication with a processed_events table. Each consumer checks if the event ID has already been processed before executing the handler.

**Rationale**: Simple and reliable. The processed_events table stores event IDs with timestamps. Before processing, the consumer performs an INSERT-or-IGNORE on the event ID. If the insert succeeds, the event is processed. If it conflicts (duplicate), the event is skipped. This works within the same database transaction as the aggregate save, ensuring atomicity.

**Alternatives considered**:
- Kafka consumer offsets only → Rejected: offset-based deduplication fails on rebalances; not truly idempotent.
- Natural key deduplication (e.g., check if ProductionOrder exists for OrderId) → Used as complementary check, but event ID deduplication is the primary mechanism for all consumers.

### R-04: Immutable Audit Trail for IATF 16949 Compliance

**Decision**: Assembly step records and inspection results are modeled as append-only event-sourced records within the aggregate. Database tables for these entities use INSERT-only semantics — no UPDATE or DELETE operations. Corrections are appended as new records with a `corrects_record_id` foreign key pointing to the original.

**Rationale**: IATF 16949 requires full traceability of quality records for 15 years. Append-only semantics ensure no data is ever lost or overwritten. The correction chain (via `corrects_record_id`) provides a clear audit trail. JPA mapping uses `@Immutable` annotation on infrastructure entities to prevent accidental updates.

**Alternatives considered**:
- Full event sourcing for all aggregates → Rejected: overkill for PoC; only quality-critical records need immutability.
- Database triggers preventing UPDATE/DELETE → Accepted as defense-in-depth alongside application-level controls.

### R-05: BOM Expansion Strategy

**Decision**: BOM expansion is a Domain Service (`BomExpansionService`) that takes a vehicle model code and selected options, then resolves the full parts list from the Vehicle Configuration context. The result is captured as a `BomSnapshot` Value Object embedded in the Production Order aggregate.

**Rationale**: Snapshotting the BOM at production order creation time decouples the production order from subsequent catalog changes. The BomSnapshot is immutable — if the vehicle configuration changes after the production order is created, the order retains its original BOM. This is a critical DDD pattern: capturing external state as a snapshot within the aggregate boundary.

**Alternatives considered**:
- Live BOM resolution at each assembly step → Rejected: catalog changes during assembly would break traceability and consistency.
- BOM as a separate aggregate → Rejected: BOM has no independent lifecycle; it's a snapshot value within the production order.

### R-06: Maven Multi-Module Dependency Enforcement

**Decision**: Use Maven module boundaries + ArchUnit tests for layer enforcement. Each bounded context has 3 modules (domain, application, infrastructure) with strict POM dependency declarations. ArchUnit tests verify no prohibited cross-layer imports exist.

**Rationale**: Maven module boundaries provide compile-time enforcement — if manufacturing-domain doesn't declare a dependency on Spring, any Spring import will fail compilation. ArchUnit provides runtime verification for finer-grained rules (e.g., no domain class importing from `..infrastructure..` package). Together they form a dual-guard system.

**Alternatives considered**:
- Gradle with `api`/`implementation` scoping → Rejected: project uses Maven per TECH.md.
- jMolecules + jMolecules ArchUnit → Considered but adds dependency to domain layer; ArchUnit alone suffices for the PoC.

### R-07: Database Strategy for PoC

**Decision**: PostgreSQL for production-like testing via Testcontainers. H2 in PostgreSQL compatibility mode for fast local development. Flyway for schema migrations. Each bounded context has its own schema namespace (e.g., `order_ctx`, `mfg_ctx`, `vehicle_ctx`).

**Rationale**: Schema-per-context provides logical separation while keeping the PoC in a single database instance. Testcontainers ensures integration tests run against real PostgreSQL. Flyway migrations provide repeatable schema setup. H2 compatibility mode allows fast developer feedback loops.

**Alternatives considered**:
- Single flat schema → Rejected: doesn't demonstrate bounded context separation at the data layer.
- Separate databases per context → Rejected: overkill for PoC; schema separation is sufficient.

### R-08: VIN Generation Strategy

**Decision**: VIN generation is a Domain Service (`VinGenerator`) in the Manufacturing domain. For the PoC, it generates valid 17-character VINs using a factory code prefix + sequential numbering + check digit calculation per ISO 3779.

**Rationale**: VIN format follows international standard ISO 3779. The generator is a domain service because VIN assignment is a manufacturing-domain concern (clarified in spec: VIN assigned at production order creation). The generator interface is defined in the domain; a simple sequential implementation lives in infrastructure.

**Alternatives considered**:
- UUID-based identifiers instead of VIN → Rejected: VIN is a domain concept with specific format rules; UUIDs are used for internal entity IDs.
- VIN from external system → Out of scope for PoC; the generator simulates this.
