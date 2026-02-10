# AutoMFG — Automotive Manufacturing DDD Proof of Concept

A hands-on **Domain-Driven Design (DDD)** project that models an automotive manufacturing order-to-delivery pipeline. Built with **Java 21**, **Spring Boot 3**, and **Hexagonal Architecture** to demonstrate how DDD tames complex business domains.

---

## Table of Contents

- [Why DDD?](#why-ddd)
- [DDD Glossary for Beginners](#ddd-glossary-for-beginners)
- [DDD Patterns in This Project](#ddd-patterns-in-this-project)
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Module Structure](#module-structure)
- [Business Rules](#business-rules)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Tech Stack](#tech-stack)

---

## Why DDD?

### The Problem: Complexity Kills Software

Imagine you're building software for an automotive manufacturer. A single vehicle order touches:

- **Sales** — a dealer places an order with specific model, color, and option packages
- **Configuration** — the system must check if "sunroof" is compatible with "convertible" (it's not!)
- **Production Planning** — a production order is created, a VIN is assigned, materials are checked
- **Assembly Line** — operators at 5 sequential workstations install parts, each requiring batch traceability
- **Quality Control** — inspectors check 6+ items per vehicle, safety failures instantly fail the car
- **Rework** — failed vehicles go back for repair, then get re-inspected

Each of these areas has its **own language**, its **own rules**, and its **own way of thinking**. When you try to build this as one big application with a shared database model, you get:

- God objects with 50+ fields that nobody understands
- Business rules scattered across controllers, services, and SQL queries
- Changes in one area breaking unrelated features
- Code that reads like a technical manual instead of the business process

### The Solution: DDD

**Domain-Driven Design** says: *"Structure your software around the business, not the technology."*

Instead of organizing code by technical layers (controllers, services, repositories), DDD organizes code by **business capabilities**:

```
 Traditional Architecture          DDD Architecture
 ========================          ================
 controllers/                      order-context/
   OrderController.java              order-domain/        ← pure business rules
   ProductionController.java         order-application/   ← use cases
   InspectionController.java         order-infrastructure/← REST, JPA, etc.
 services/                         manufacturing-context/
   OrderService.java                 manufacturing-domain/
   ProductionService.java            manufacturing-application/
   InspectionService.java            manufacturing-infrastructure/
 repositories/                     vehicle-config-context/
   OrderRepository.java              ...
   ProductionRepository.java
```

The result? **Each bounded context** can evolve independently. The Order team doesn't need to understand assembly line sequencing. The Quality team doesn't need to know about dealer pricing. And the domain logic — the most valuable part of your software — stays **pure, testable, and framework-free**.

---

## DDD Glossary for Beginners

### Strategic Design (The Big Picture)

| Term | What It Means | Example in This Project |
|------|---------------|------------------------|
| **Domain** | The subject area your software is about | Automotive manufacturing: orders, production, quality |
| **Subdomain** | A smaller, focused area within the domain | Order Management, Manufacturing, Vehicle Configuration |
| **Bounded Context** | A clear boundary where a specific model applies. Same word can mean different things in different contexts | "Order" in Sales = customer request; "Order" in Manufacturing = production instruction |
| **Ubiquitous Language** | The shared vocabulary between developers and domain experts | We say "VIN" not "vehicle_id_string"; "Assembly Step" not "task_record" |
| **Context Map** | How bounded contexts relate to each other | Order Context publishes `OrderPlacedEvent` → Manufacturing Context consumes it |

### Tactical Design (The Building Blocks)

| Term | What It Means | Example in This Project |
|------|---------------|------------------------|
| **Entity** | An object with a unique identity that persists over time | `AssemblyStep` — each step has its own ID and tracks completion |
| **Value Object** | An object defined by its attributes, not identity. Immutable. | `VIN("1HGCM82633A004352")` — two VINs with the same string are equal |
| **Aggregate** | A cluster of entities/value objects treated as a single unit with a root entity | `ProductionOrder` is the root; it owns `AssemblyProcess`, which owns `AssemblyStep`s |
| **Aggregate Root** | The entry point to an aggregate. All changes go through it. | You never modify an `AssemblyStep` directly — you call `productionOrder.completeAssemblyStep()` |
| **Domain Event** | Something important that happened in the domain | `OrderPlacedEvent`, `AssemblyCompletedEvent`, `InspectionFailedEvent` |
| **Domain Service** | Business logic that doesn't naturally belong to a single entity | `BomExpansionService` — expands a Bill of Materials across multiple parts |
| **Repository** | An interface for loading/saving aggregates (defined in domain, implemented in infrastructure) | `ProductionOrderRepository` — the domain says "I need to save this"; the infrastructure decides *how* |
| **Factory** | A method that encapsulates complex object creation | `Order.place(...)` — creates an order with validation, delivery date calculation, and event registration |

### Architectural Patterns

| Term | What It Means | Example in This Project |
|------|---------------|------------------------|
| **Hexagonal Architecture** | The domain is at the center. Frameworks and databases are pluggable adapters on the outside. | Domain layer has **zero** Spring or JPA imports |
| **Port** | An interface defined by the domain that the outside world must implement | `OrderRepository`, `VehicleConfigGateway`, `MaterialAvailabilityGateway` |
| **Adapter** | An implementation of a port using a specific technology | `JpaOrderRepositoryAdapter` implements `OrderRepository` using Spring Data JPA |
| **Anti-Corruption Layer (ACL)** | A translation layer that prevents one context's model from leaking into another | `VehicleConfigACLAdapter` — the Order context queries vehicle config data without importing vehicle-config domain classes |
| **Transactional Outbox** | A pattern for reliable event publishing: write events to a DB table, then relay them | `DomainEventOutbox` table — events are stored in the same transaction as the aggregate |

---

## DDD Patterns in This Project

### 1. Aggregate Root with Domain Events

The `Order` aggregate encapsulates all order business rules. Changes go through the root, and domain events are registered for downstream consumers:

```java
// order-domain — pure Java, no frameworks
public class Order extends AggregateRoot {

    public static Order place(OrderId id, OrderNumber orderNumber, ...) {
        // BR-03: delivery date must be at least 45 days from now
        if (estimatedDeliveryDate.isBefore(LocalDate.now().plusDays(45))) {
            throw new IllegalArgumentException("Delivery date must be >= 45 days");
        }
        Order order = new Order(id, orderNumber, ...);
        order.registerEvent(new OrderPlacedEvent(...));  // ← domain event
        return order;
    }

    public void changeConfiguration(String newColor, List<String> newOptions, ...) {
        // BR-15: max 3 changes allowed
        if (this.changeCount >= 3) {
            throw new IllegalStateException("Maximum changes reached");
        }
        // ... apply changes ...
        this.changeCount++;
        registerEvent(new OrderChangedEvent(...));
    }
}
```

### 2. Value Objects as Java 21 Records

Value Objects are immutable and validated at construction. Java 21 records make them concise:

```java
// A VIN (Vehicle Identification Number) is a Value Object
public record VIN(String value) {
    public VIN {
        if (!value.matches("[A-HJ-NPR-Z0-9]{17}")) {
            throw new IllegalArgumentException("Invalid VIN: " + value);
        }
    }
}

// Two VINs with the same value are considered equal — that's a Value Object
VIN a = new VIN("1HGCM82633A004352");
VIN b = new VIN("1HGCM82633A004352");
assert a.equals(b);  // true!
```

### 3. Repository Port (Hexagonal Architecture)

The domain defines *what* it needs. The infrastructure decides *how*:

```java
// Domain layer — just an interface, no JPA, no Spring
public interface ProductionOrderRepository {
    ProductionOrder save(ProductionOrder order);
    Optional<ProductionOrder> findById(ProductionOrderId id);
    boolean existsBySourceOrderId(UUID sourceOrderId);
}

// Infrastructure layer — the JPA implementation
@Repository
public class JpaProductionOrderRepositoryAdapter implements ProductionOrderRepository {
    private final ProductionOrderJpaRepository jpaRepo;
    private final ProductionOrderMapper mapper;

    @Override
    public ProductionOrder save(ProductionOrder order) {
        ProductionOrderJpaEntity entity = mapper.toJpaEntity(order);
        return mapper.toDomain(jpaRepo.save(entity));
    }
}
```

### 4. Anti-Corruption Layer (ACL)

The Order context needs vehicle configuration data, but it doesn't import the Vehicle Config domain. Instead, it defines its own gateway interface and uses native SQL:

```java
// Order domain defines what it needs (no knowledge of vehicle-config internals)
public interface VehicleConfigGateway {
    ValidationResult validateConfiguration(String modelCode, String colorCode, List<String> optionCodes);
    BigDecimal calculatePrice(String modelCode, List<String> optionCodes);
}

// Order infrastructure implements it with its own queries
@Service
public class VehicleConfigACLAdapter implements VehicleConfigGateway {
    // Uses native SQL — no imports from vehicle-config module
    // This IS the anti-corruption layer
}
```

### 5. Domain Events Across Bounded Contexts

When an order is placed, the Manufacturing context needs to create a production order. But the Order context doesn't call Manufacturing directly — it publishes an event:

```
Order Context                    Manufacturing Context
─────────────                    ─────────────────────
Order.place()
  → registers OrderPlacedEvent
  → SpringDomainEventPublisher
       publishes event ─────────→ OrderEventConsumer
                                   → checks ProcessedEvent (idempotency)
                                   → calls CreateProductionOrderUseCase
                                   → ProductionOrder.create()
                                      → assigns VIN
                                      → expands BOM
                                      → registers ProductionOrderScheduledEvent
```

### 6. Immutable Audit Trail

Assembly steps and inspection results cannot be modified after recording — a requirement for automotive traceability (IATF 16949):

```java
public class AssemblyStep {
    public void complete(String operatorId, String materialBatchId, int actualMinutes) {
        if (this.status == AssemblyStepStatus.COMPLETED) {
            throw new IllegalStateException("Step already completed — records are immutable");
        }
        // Once set, these fields can never change
        this.operatorId = operatorId;
        this.materialBatchId = new MaterialBatchId(materialBatchId);
        this.status = AssemblyStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
```

### 7. Four-Eyes Principle (Domain Rule in the Aggregate)

Quality inspection requires a reviewer different from the inspector — this business rule lives in the domain, not in a controller:

```java
public class QualityInspection extends AggregateRoot {
    public void review(String reviewerId) {
        if (reviewerId.equals(this.inspectorId)) {
            throw new IllegalArgumentException("Reviewer must differ from inspector (four-eyes principle)");
        }
        // ...
    }
}
```

---

## Project Overview

This project models an **end-to-end vehicle manufacturing pipeline**:

```
Dealer Places Order ──→ Production Order Created ──→ Assembly Line ──→ Quality Inspection ──→ Vehicle Completed
     (US-01)                  (US-02)                  (US-03)              (US-04)
                                                                     ↓ (if failed)
                                                                  Rework ──→ Re-inspection
```

### User Stories

| # | Story | Priority | Description |
|---|-------|----------|-------------|
| US-01 | Place Order | P1 | Dealer selects model/color/options, system validates compatibility, calculates price and delivery date |
| US-02 | Create Production Order | P1 | System auto-creates production order with VIN, expands BOM, checks materials |
| US-03 | Advance Assembly | P1 | Operator scans at workstations, completes tasks with batch numbers, sequential enforcement |
| US-04 | Quality Inspection | P2 | Inspector checks items, safety-fail logic, four-eyes review, rework flow |
| US-05 | Change Order | P2 | Dealer changes color/options before production starts; model change = cancel + new |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Bootstrap Module                      │
│         Spring Boot App / Flyway / Docker Compose         │
└────────────────────────┬────────────────────────────────┘
                         │ depends on all infrastructure modules
    ┌────────────────────┼────────────────────┐
    │                    │                    │
    ▼                    ▼                    ▼
┌─────────┐      ┌──────────────┐     ┌──────────────┐
│  Order   │      │Manufacturing │     │Vehicle Config │
│ Context  │      │   Context    │     │   Context     │
└────┬─────┘      └──────┬───────┘     └──────┬───────┘
     │                   │                    │
     ▼                   ▼                    ▼

Each context has 3 layers:

┌─────────────────────────────────────┐
│        Infrastructure Layer          │  ← REST controllers, JPA entities,
│  (Spring Boot, JPA, REST adapters)   │     event consumers, ACL adapters
├─────────────────────────────────────┤
│         Application Layer            │  ← Use cases (PlaceOrderUseCase,
│    (Use cases, no framework deps)    │     CreateProductionOrderUseCase)
├─────────────────────────────────────┤
│           Domain Layer               │  ← Aggregates, Entities, Value Objects,
│  (Pure Java — ZERO framework deps)   │     Domain Events, Domain Services, Ports
└─────────────────────────────────────┘
```

**ArchUnit tests** automatically enforce that domain and application layers have zero Spring/JPA imports.

---

## Module Structure

```
auto-mfg/
├── pom.xml                              # Parent POM (Java 21, Spring Boot 3.3.7)
├── docker-compose.yml                   # PostgreSQL + Kafka
│
├── shared-kernel/                       # Base types shared across contexts
│   └── src/main/java/
│       └── com/automfg/shared/
│           ├── domain/
│           │   ├── AggregateRoot.java       # Base class with domain events
│           │   ├── DomainEvent.java         # Base event with ID + timestamp
│           │   └── DomainEventPublisher.java# Port interface
│           └── infrastructure/
│               ├── ProcessedEvent.java      # Idempotent event tracking
│               ├── DomainEventOutbox.java   # Transactional outbox
│               └── SpringDomainEventPublisher.java
│
├── order-context/
│   ├── order-domain/                    # Pure Java — Order aggregate
│   │   ├── model/   Order, OrderId, OrderNumber, OrderStatus
│   │   ├── event/   OrderPlacedEvent, OrderChangedEvent
│   │   └── port/    OrderRepository, VehicleConfigGateway
│   ├── order-application/               # Use cases
│   │   └── usecase/ PlaceOrderUseCase, ChangeOrderUseCase
│   └── order-infrastructure/            # Spring/JPA adapters
│       ├── persistence/  JPA entities, mappers, repository adapters
│       └── adapter/      OrderController (REST), VehicleConfigACL
│
├── manufacturing-context/
│   ├── manufacturing-domain/            # Pure Java — core manufacturing
│   │   ├── model/   ProductionOrder, AssemblyProcess, AssemblyStep,
│   │   │            QualityInspection, InspectionItem, ReworkOrder,
│   │   │            VIN, BomSnapshot, 15+ value objects
│   │   ├── event/   14 domain events (scheduled, started, completed, etc.)
│   │   ├── service/ BomExpansionService, InspectionCompletionService
│   │   └── port/    5 repository/gateway interfaces
│   ├── manufacturing-application/       # Use cases
│   │   └── usecase/ Create/Start/Complete production, inspection, rework
│   └── manufacturing-infrastructure/    # Spring/JPA adapters
│       ├── persistence/  JPA entities, mappers for all aggregates
│       └── adapter/      ProductionOrderController, InspectionController,
│                         ReworkController, OrderEventConsumer
│
├── vehicle-config-context/
│   ├── vehicle-config-domain/           # Configuration rules
│   ├── vehicle-config-application/
│   └── vehicle-config-infrastructure/   # JPA entities for config data
│
├── material-context/
│   └── material-mock/                   # Mock adapter (always available)
│
└── bootstrap/                           # Spring Boot entry point
    ├── src/main/java/     AutoMfgApplication.java
    ├── src/main/resources/
    │   ├── application.yml              # H2 (dev) / PostgreSQL (prod)
    │   └── db/migration/
    │       ├── V1__init_schema.sql      # All tables
    │       └── V2__seed_data.sql        # Vehicle models, colors, options
    └── src/test/java/
        └── architecture/ArchitectureTest.java  # 5 ArchUnit rules
```

---

## Business Rules

| Rule | Description | Where It's Enforced |
|------|-------------|---------------------|
| BR-01 | Max 50 pending orders per dealer per vehicle model | `PlaceOrderUseCaseImpl` |
| BR-02 | Option package compatibility validation | `VehicleConfiguration.validateOptions()` |
| BR-03 | Delivery date >= order date + 45 days | `Order.place()` |
| BR-07 | Assembly stations must be completed sequentially | `AssemblyProcess.completeStep()` |
| BR-08 | Material batch ID required for every assembly step | `AssemblyStep.complete()` |
| BR-09 | Overtime alert when actual time > 150% standard time | `ProductionOrder.completeAssemblyStep()` |
| BR-10 | Any safety-critical item failure = inspection FAILED | `QualityInspection.complete()` |
| BR-11 | Max 3 conditional non-safety items for CONDITIONAL_PASS | `QualityInspection.complete()` |
| BR-12 | Four-eyes principle: reviewer != inspector | `QualityInspection.review()` |
| BR-14 | Model change = cancel existing + create new order | `ChangeOrderUseCaseImpl` |
| BR-15 | Maximum 3 order changes allowed | `Order.changeConfiguration()` |

---

## API Endpoints

### Order Management

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/orders` | Place a new vehicle order |
| `GET` | `/api/v1/orders` | List orders (filter by dealerId, status) |
| `GET` | `/api/v1/orders/{id}` | Get order details |
| `POST` | `/api/v1/orders/{id}/changes` | Change order configuration |

### Manufacturing — Production & Assembly

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/production-orders` | List production orders (filter by status) |
| `GET` | `/api/v1/production-orders/{id}` | Get production order with BOM and progress |
| `POST` | `/api/v1/production-orders/{id}/start` | Start production (operator scan) |
| `GET` | `/api/v1/production-orders/{id}/assembly-steps` | Get assembly steps (filter by station) |
| `POST` | `/api/v1/production-orders/{id}/assembly-steps/{stepId}/complete` | Complete an assembly step |

### Manufacturing — Quality Inspection

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/inspections` | Create inspection for assembled vehicle |
| `GET` | `/api/v1/inspections/{id}` | Get inspection details with items |
| `POST` | `/api/v1/inspections/{id}/items/{itemId}/result` | Record item result |
| `POST` | `/api/v1/inspections/{id}/complete` | Complete inspection (evaluate result) |
| `POST` | `/api/v1/inspections/{id}/review` | Four-eyes review |
| `POST` | `/api/v1/rework-orders/{id}/complete` | Complete rework |

---

## Getting Started

### Prerequisites

- **Java 21** — `sdk install java 21.0.5-tem` (via [SDKMAN](https://sdkman.io/))
- **Maven 3.9+** — `sdk install maven`
- **Docker** — for PostgreSQL and Kafka (optional for local dev with H2)

### Quick Start (H2 in-memory)

```bash
cd auto-mfg

# Build all modules
mvn clean verify

# Run the application (uses H2 by default)
cd bootstrap
mvn spring-boot:run
```

Open http://localhost:8080/swagger-ui.html for the API documentation.

### With PostgreSQL + Kafka

```bash
cd auto-mfg

# Start infrastructure
docker compose up -d

# Run with PostgreSQL profile
cd bootstrap
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## Running Tests

```bash
cd auto-mfg

# All tests (unit + architecture)
mvn clean verify

# Domain tests only (fast, no Spring context)
mvn test -pl order-context/order-domain
mvn test -pl manufacturing-context/manufacturing-domain

# ArchUnit architecture tests
mvn test -pl bootstrap -Dtest=ArchitectureTest
```

### Test Summary (45 tests)

| Test Class | Count | What It Validates |
|------------|-------|-------------------|
| `OrderTest` | 10 | Place, change, cancel, status transitions, BR-03/BR-15 |
| `ProductionOrderTest` | 8 | Create, start, assembly completion, overtime alerts |
| `AssemblyProcessTest` | 5 | Station sequencing (BR-07), batch required (BR-08) |
| `QualityInspectionTest` | 13 | Safety fail (BR-10), conditional pass (BR-11), four-eyes (BR-12) |
| `BomExpansionServiceTest` | 2 | BOM expansion with available/missing materials |
| `InspectionCompletionServiceTest` | 2 | Cross-aggregate inspection result propagation |
| `ArchitectureTest` | 5 | Domain/application layers have zero Spring/JPA deps |

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Records for Value Objects, modern language features |
| Spring Boot 3.3.7 | REST controllers, dependency injection, event publishing |
| Spring Data JPA | Repository implementations |
| H2 / PostgreSQL | Persistence (H2 for dev, PostgreSQL for production) |
| Flyway | Database migrations and seed data |
| ArchUnit | Architecture compliance testing |
| JUnit 5 + AssertJ | Domain unit testing |
| SpringDoc OpenAPI | Swagger UI for API documentation |
| Docker Compose | PostgreSQL + Kafka infrastructure |

---

## Further Reading

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.dddcommunity.org/book/evans_2003/) — Eric Evans (the "Blue Book")
- [Implementing Domain-Driven Design](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577) — Vaughn Vernon (the "Red Book")
- [Architecture Patterns with Python](https://www.cosmicpython.com/) — Harry Percival & Bob Gregory (free online, great intro to Ports & Adapters)

---

## License

This project is a proof of concept for educational purposes.
