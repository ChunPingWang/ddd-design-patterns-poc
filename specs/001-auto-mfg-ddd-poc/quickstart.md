# Quickstart: AutoMFG DDD PoC

**Feature**: 001-auto-mfg-ddd-poc
**Date**: 2026-02-11

## Prerequisites

- Java 21 (LTS) — [SDKMAN](https://sdkman.io/) recommended: `sdk install java 21.0.5-tem`
- Maven 3.9+ — `sdk install maven`
- Docker & Docker Compose — for PostgreSQL and Kafka
- IDE: IntelliJ IDEA recommended (import as Maven multi-module project)

## Project Setup

```bash
# Clone and checkout feature branch
git clone <repo-url>
cd ddd-design-patterns-poc
git checkout 001-auto-mfg-ddd-poc

# Start infrastructure (PostgreSQL + Kafka)
docker compose up -d

# Build all modules
mvn clean install

# Run the application
cd bootstrap
mvn spring-boot:run
```

## Module Structure

```
auto-mfg/
├── shared-kernel/                  # Base types: DomainEvent, common VOs
├── order-context/
│   ├── order-domain/               # Pure Java — Order aggregate, events, ports
│   ├── order-application/          # Use cases — PlaceOrder, ChangeOrder
│   └── order-infrastructure/       # REST, JPA, Kafka adapters
├── manufacturing-context/
│   ├── manufacturing-domain/       # Pure Java — ProductionOrder, QualityInspection
│   ├── manufacturing-application/  # Use cases — CreateProductionOrder, CompleteStep
│   └── manufacturing-infrastructure/
├── vehicle-config-context/
│   ├── vehicle-config-domain/      # VehicleConfiguration, CompatibilityRules
│   ├── vehicle-config-application/
│   └── vehicle-config-infrastructure/
├── material-context/
│   └── material-mock/              # Mock adapter for material availability
└── bootstrap/                      # Spring Boot main, config, migrations, seed data
```

## Development Workflow (TDD)

Follow the constitution-mandated Red-Green-Refactor cycle:

### 1. Write Failing Domain Test (Red)
```bash
# Example: testing a new business rule in ProductionOrder
cd manufacturing-context/manufacturing-domain
mvn test  # Should see RED (failing test)
```

### 2. Implement Domain Logic (Green)
```bash
mvn test  # Should see GREEN
```

### 3. Refactor
```bash
mvn test  # Still GREEN after refactoring
```

### 4. Write Failing Integration Test (Red)
```bash
cd manufacturing-context/manufacturing-infrastructure
mvn test -Dtest=JpaProductionOrderRepositoryTest  # RED
```

### 5. Implement Adapter (Green)
```bash
mvn test  # GREEN
```

### 6. Run Full Build
```bash
# From project root
mvn clean verify  # Runs unit + integration + ArchUnit tests
```

## Running Tests

```bash
# All tests (unit + integration + architecture)
mvn clean verify

# Domain tests only (fast, no Spring context)
mvn test -pl manufacturing-context/manufacturing-domain

# ArchUnit architecture tests
mvn test -pl bootstrap -Dtest=ArchitectureTest

# Integration tests with Testcontainers
mvn verify -pl manufacturing-context/manufacturing-infrastructure

# E2E tests
mvn verify -pl bootstrap -Dtest=*E2ETest
```

## Key URLs (Local Dev)

| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| OpenAPI Docs | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 (db: automfg, user: automfg) |
| Kafka | localhost:9092 |

## Seed Data

The bootstrap module loads seed data on startup:
- **Vehicle Models**: MODEL-X-SEDAN, MODEL-Y-SUV, MODEL-Z-CONVERTIBLE
- **Colors**: SILVER, BLACK, WHITE, RED, BLUE
- **Option Packages**: LUXURY-INTERIOR, PREMIUM-AUDIO, SPORT-SUSPENSION, SUNROOF, NAVIGATION
- **Compatibility Rules**: SUNROOF incompatible with MODEL-Z-CONVERTIBLE
- **Inspection Checklists**: Per model, with safety-critical items flagged
- **Workstations**: WS-01 (Body), WS-02 (Paint), WS-03 (Engine), WS-04 (Interior), WS-05 (Final Assembly)
- **Dealers**: DEALER-001, DEALER-002

## Architecture Rules

The domain layer MUST remain pure Java with zero framework dependencies:

```
manufacturing-domain/pom.xml:
  - Only commons-lang3 allowed (if needed)
  - NO Spring, NO JPA, NO Kafka dependencies

manufacturing-application/pom.xml:
  - Depends on manufacturing-domain only
  - NO Spring, NO infrastructure dependencies

manufacturing-infrastructure/pom.xml:
  - Depends on manufacturing-application (transitively gets domain)
  - Spring Boot, JPA, Kafka dependencies allowed here
```

ArchUnit tests in `bootstrap/src/test/java/.../architecture/ArchitectureTest.java` automatically enforce these rules on every build.

## Common Tasks

### Add a new business rule
1. Write a failing domain test
2. Implement rule in the aggregate or domain service
3. Run `mvn test -pl manufacturing-context/manufacturing-domain`

### Add a new API endpoint
1. Define the use case interface (input port) in application layer
2. Implement the use case
3. Create REST controller (inbound adapter) in infrastructure layer
4. Add OpenAPI annotations

### Add a new domain event
1. Define event class in domain `event/` package (extend DomainEvent)
2. Raise event from aggregate method
3. Add event consumer in the target context's infrastructure layer
4. Ensure consumer is idempotent (check ProcessedEvent table)
