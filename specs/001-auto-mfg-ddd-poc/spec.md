# Feature Specification: AutoMFG — Automotive Manufacturing Order-to-Delivery PoC

**Feature Branch**: `001-auto-mfg-ddd-poc`
**Created**: 2026-02-11
**Status**: Draft
**Input**: PRD for automotive manufacturing DDD PoC covering order-to-delivery flow with 5 core use cases across 4 bounded contexts

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dealer Places a Vehicle Order (Priority: P1)

A dealer logs into the system, selects a vehicle model, color, and option packages, and submits an order. The system validates that the selected options are compatible (e.g., a sunroof cannot be paired with a convertible model), calculates an estimated delivery date and price quote, and creates the order. The order is published as an event so downstream processes (production scheduling) can react.

**Why this priority**: This is the entry point for the entire order-to-delivery flow. Without order placement, no downstream process can be triggered. It validates the core domain model's ability to encapsulate business rules (compatibility checks, order limits, delivery date calculation).

**Independent Test**: Can be fully tested by having a dealer submit orders with various vehicle configurations and verifying that business rules are enforced, orders are persisted, and events are published.

**Acceptance Scenarios**:

1. **Given** a dealer is authenticated and selects Model-X Sedan with Luxury Interior Package in Silver, **When** the dealer confirms the order, **Then** the system creates the order with status `PLACED`, calculates an estimated delivery date at least 45 days from today, and publishes an `OrderPlacedEvent`.
2. **Given** a dealer selects a sunroof option for a convertible model, **When** the dealer attempts to confirm the order, **Then** the system rejects the order with a compatibility error explaining the conflict.
3. **Given** a dealer already has 50 pending orders for the same vehicle model, **When** the dealer attempts to place another order for that model, **Then** the system rejects the order citing the maximum pending order limit.
4. **Given** a dealer selects valid model, color, and options, **When** the order is confirmed, **Then** the system returns a price quote reflecting all selected options.

---

### User Story 2 - System Creates a Production Order from a Placed Order (Priority: P1)

When an order is placed, the system automatically creates a production order. It resolves the vehicle configuration into a full bill of materials (BOM), checks material availability, and either schedules the production order or marks it as pending materials. If materials are insufficient, a shortage event is published to notify material management.

**Why this priority**: This is the critical link between the order and manufacturing domains. It validates event-driven cross-context communication and BOM expansion — both core DDD patterns being proven by this PoC.

**Independent Test**: Can be tested by publishing an `OrderPlacedEvent` and verifying that a production order is created with the correct BOM, material availability is checked, and the appropriate status is assigned.

**Acceptance Scenarios**:

1. **Given** an `OrderPlacedEvent` is received for a vehicle with all materials available, **When** the system processes the event, **Then** a production order is created with status `SCHEDULED` and a complete BOM breakdown.
2. **Given** an `OrderPlacedEvent` is received but certain materials are insufficient, **When** the system processes the event, **Then** a production order is created with status `MATERIAL_PENDING` and a `MaterialShortageEvent` is published listing the missing materials.
3. **Given** an `OrderPlacedEvent` is received, **When** the production order is created, **Then** it maintains a 1:1 relationship with the originating customer order.

---

### User Story 3 - Production Line Operator Advances Assembly (Priority: P1)

A production line operator scans a production order barcode at their workstation. The system displays the assembly task list for that station. The operator completes each task, recording the material batch numbers used. When all tasks at the station are done, the system advances the order to the next station. At the final station, the order status becomes assembly completed.

**Why this priority**: This is the core manufacturing domain — the primary competitive advantage area. It validates the aggregate design for production workflow, sequential station enforcement, and material traceability (a regulatory requirement).

**Independent Test**: Can be tested by simulating an operator scanning into sequential workstations, completing tasks with batch number recording, and verifying correct station progression and traceability records.

**Acceptance Scenarios**:

1. **Given** a production order with status `IN_PRODUCTION` at Station 3, **When** the operator scans the barcode at Station 3, **Then** the system displays the assembly task list for Station 3.
2. **Given** an operator is at Station 3 with 4 assembly tasks, **When** the operator completes all 4 tasks recording batch numbers for each, **Then** the system automatically advances the order to Station 4.
3. **Given** an operator attempts to scan into Station 5 while the order is still at Station 3, **When** the scan is processed, **Then** the system rejects the operation with an error indicating stations must be completed in sequence.
4. **Given** an operator is at the final workstation and completes all tasks, **When** the last task is recorded, **Then** the production order status changes to `ASSEMBLY_COMPLETED`.
5. **Given** an operator has been working at a station beyond 150% of the standard cycle time, **When** the threshold is exceeded, **Then** the system raises an overtime alert.
6. **Given** an assembly task requires a specific part, **When** the operator records completion, **Then** the system requires and stores the material batch number for traceability.

---

### User Story 4 - Quality Inspector Performs Vehicle Inspection (Priority: P2)

After assembly is completed, a quality inspector receives the vehicle for inspection. The system loads the inspection checklist for that vehicle model. The inspector checks each item and records the result (pass, fail, or conditional pass). If all items pass, the vehicle is marked as inspection passed and a completion event is published. If safety-critical items fail, the entire vehicle fails inspection and a rework order is created.

**Why this priority**: Quality inspection is essential for regulatory compliance (ISO 9001 / IATF 16949) and triggers the downstream logistics flow. However, it depends on assembly completion (P1) being implemented first.

**Independent Test**: Can be tested by presenting a completed vehicle to a quality inspector, running through the checklist, and verifying correct pass/fail logic and rework order creation.

**Acceptance Scenarios**:

1. **Given** a vehicle with status `ASSEMBLY_COMPLETED`, **When** the quality inspector checks all items and all pass, **Then** the vehicle status changes to `INSPECTION_PASSED` and a `VehicleCompletedEvent` is published.
2. **Given** a vehicle undergoing inspection, **When** a safety-critical item is marked as failed, **Then** the entire vehicle is marked `INSPECTION_FAILED` regardless of other results, and a rework order is created.
3. **Given** a vehicle undergoing inspection with 3 non-safety items marked as "conditional pass" and all others passing, **When** the inspector submits the results, **Then** the vehicle passes inspection (maximum 3 conditional passes allowed for non-safety items).
4. **Given** a vehicle undergoing inspection with 4 non-safety items marked as "conditional pass," **When** the inspector submits the results, **Then** the vehicle fails inspection (exceeds the 3 conditional pass limit).
5. **Given** an inspection has been completed by one inspector, **When** the results are submitted, **Then** the system requires a different inspector to review and confirm the results before finalizing (four-eyes principle).

---

### User Story 5 - Dealer Changes an Existing Order (Priority: P2)

A dealer submits a change request for an existing order — such as changing the vehicle color or option packages. The system checks whether the change is feasible based on the current production status. If production has not started, the change is applied to both the order and production order. If production is already underway, the change is rejected. Model changes are treated as order cancellation plus new order creation.

**Why this priority**: Order changes are a common real-world scenario that tests the system's ability to coordinate across bounded contexts (order and manufacturing). It validates aggregate consistency under concurrent modification.

**Independent Test**: Can be tested by submitting change requests against orders in various statuses and verifying correct acceptance/rejection behavior and event publishing.

**Acceptance Scenarios**:

1. **Given** an order with status `PLACED` and 0 previous changes, **When** the dealer changes the color from Silver to Black, **Then** the order and associated production order are updated, and an `OrderChangedEvent` is published.
2. **Given** an order with status `SCHEDULED` (production not yet started), **When** the dealer changes option packages, **Then** the change is accepted and the production order BOM is re-expanded.
3. **Given** an order whose production order is `IN_PRODUCTION`, **When** the dealer submits a change request, **Then** the system rejects the change with a message indicating production has already started.
4. **Given** a dealer wants to change the vehicle model on an existing order, **When** the change is submitted, **Then** the system cancels the original order and creates a new order for the new model.
5. **Given** an order that has already been changed 3 times, **When** the dealer submits a 4th change request, **Then** the system rejects the change citing the maximum change limit.

---

### Edge Cases

- What happens when two dealers simultaneously place orders that would exceed the 50-order limit for the same model? The system must enforce the limit under concurrent access, allowing one and rejecting the other.
- What happens when materials become available after a production order was marked `MATERIAL_PENDING`? The system should react to material availability updates and transition the production order to `SCHEDULED`.
- What happens when an `OrderChangedEvent` arrives but the production order has already advanced past `SCHEDULED`? The manufacturing context must reject the change and notify the order context.
- How does the system handle a quality inspector attempting to inspect a vehicle that is still in assembly? The system must enforce status prerequisites and reject the inspection attempt.
- What happens if the four-eyes review inspector disagrees with the original inspection result? The system should flag the discrepancy for resolution before finalizing.
- What happens when a production order's assembly step fails to record a batch number? The system must not allow the step to be marked complete without traceability data.

## Requirements *(mandatory)*

### Functional Requirements

**Order Management Context**:

- **FR-001**: System MUST allow dealers to place orders by selecting a vehicle model, color, and option packages.
- **FR-002**: System MUST validate option compatibility before accepting an order (e.g., sunroof incompatible with convertible).
- **FR-003**: System MUST enforce a maximum of 50 pending orders per dealer per vehicle model.
- **FR-004**: System MUST calculate an estimated delivery date no earlier than 45 days from the order date.
- **FR-005**: System MUST calculate and present a price quote based on selected model and options.
- **FR-006**: System MUST publish an `OrderPlacedEvent` when an order is successfully created.
- **FR-007**: System MUST allow dealers to change color or option packages on orders whose production has not started.
- **FR-008**: System MUST reject order changes when the associated production order is `IN_PRODUCTION` or beyond.
- **FR-009**: System MUST treat vehicle model changes as cancellation of the original order plus creation of a new order.
- **FR-010**: System MUST enforce a maximum of 3 option/color changes per order.
- **FR-011**: System MUST publish an `OrderChangedEvent` when an order is successfully modified.

**Manufacturing Management Context**:

- **FR-012**: System MUST automatically create a production order upon receiving an `OrderPlacedEvent`.
- **FR-013**: System MUST expand the vehicle configuration into a complete BOM for each production order.
- **FR-014**: System MUST check material availability and set production order status to `SCHEDULED` (sufficient) or `MATERIAL_PENDING` (insufficient).
- **FR-015**: System MUST publish a `MaterialShortageEvent` when materials are insufficient.
- **FR-016**: System MUST enforce a 1:1 relationship between customer orders and production orders.
- **FR-017**: System MUST enforce sequential workstation progression — no station skipping allowed.
- **FR-018**: System MUST require material batch number recording for every assembly step.
- **FR-019**: System MUST automatically advance the production order to the next station when all tasks at the current station are completed.
- **FR-020**: System MUST transition the production order to `ASSEMBLY_COMPLETED` when the final station is completed.
- **FR-021**: System MUST raise an alert when a single workstation operation exceeds 150% of standard cycle time.

**Quality Inspection**:

- **FR-022**: System MUST load a model-specific inspection checklist when a vehicle enters quality inspection.
- **FR-023**: System MUST mark the entire vehicle as `INSPECTION_FAILED` if any safety-critical item fails.
- **FR-024**: System MUST allow a maximum of 3 non-safety items with "conditional pass" status for the vehicle to pass overall.
- **FR-025**: System MUST require a different inspector to review and confirm inspection results (four-eyes principle).
- **FR-026**: System MUST publish a `VehicleCompletedEvent` when a vehicle passes inspection.
- **FR-027**: System MUST create a rework order when a vehicle fails inspection.

**Cross-Context Communication**:

- **FR-028**: System MUST use domain events for communication between bounded contexts (Order, Manufacturing, Material, Logistics).
- **FR-029**: System MUST translate order context vocabulary to manufacturing context vocabulary through an anti-corruption layer when referencing vehicle configurations.

### Key Entities

- **Order**: A dealer's request to purchase a specific vehicle configuration. Key attributes: order ID, dealer, vehicle model, color, option packages, status (PLACED, SCHEDULED, IN_PRODUCTION, COMPLETED, CANCELLED), estimated delivery date, price quote, change count.
- **Vehicle Configuration**: A specification defining a vehicle model's available colors, option packages, and compatibility rules between options. Referenced by orders through an anti-corruption layer.
- **Production Order**: A manufacturing instruction to build a specific vehicle. Key attributes: production order ID, linked order ID, BOM, status (SCHEDULED, MATERIAL_PENDING, IN_PRODUCTION, ASSEMBLY_COMPLETED, INSPECTION_PASSED, INSPECTION_FAILED), current workstation.
- **BOM (Bill of Materials)**: The complete list of parts and materials required to assemble a specific vehicle configuration. Expanded from vehicle configuration during production order creation.
- **Workstation**: A defined position on the assembly line where specific assembly tasks are performed in sequence.
- **Assembly Step**: A single task performed at a workstation, recording the operator, timestamp, and material batch numbers used.
- **Quality Inspection**: An evaluation of a completed vehicle against a model-specific checklist. Includes individual item results (pass/fail/conditional) and requires dual-inspector review.
- **Inspection Checklist**: A model-specific list of inspection items, each categorized as safety-critical or non-safety.
- **Rework Order**: A repair instruction created when a vehicle fails quality inspection, referencing the failed inspection items.
- **Dealer**: An authorized vehicle sales partner who places and manages orders.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of business rules (BR-01 through BR-15) are enforced within the domain model, not in service or presentation layers — verified by inspecting where rule logic resides.
- **SC-002**: Domain layer achieves at least 90% test coverage, with all business rules having dedicated test cases.
- **SC-003**: Zero architectural violations — the domain layer has no dependencies on infrastructure components, verified through dependency analysis.
- **SC-004**: All 5 core use cases (order placement, production order creation, assembly advancement, quality inspection, order change) pass end-to-end scenario tests.
- **SC-005**: Dealers can complete order placement (model selection, option validation, confirmation) in under 2 seconds of system processing time.
- **SC-006**: Workstation barcode scanning and task display completes in under 500 milliseconds.
- **SC-007**: Cross-context event propagation (e.g., OrderPlaced to ProductionOrder creation) completes within a single business transaction cycle.
- **SC-008**: Material batch traceability data is recorded for 100% of assembly steps, enabling full defect batch tracing.
- **SC-009**: Quality inspection four-eyes review is enforced — no vehicle can pass inspection without two independent inspector confirmations.
- **SC-010**: The system correctly prevents order changes for production orders that have progressed beyond `SCHEDULED` status in 100% of test scenarios.

## Assumptions

- Material Management context will be mocked for this PoC — material availability checks will use predefined test data rather than a live inventory system.
- Logistics context is out of scope — `VehicleCompletedEvent` will be published but no logistics processing will be implemented (event stub only).
- User authentication and authorization are out of scope — all actors are assumed to be pre-authenticated.
- Vehicle model catalog, option packages, and compatibility rules will use seed data defined for the PoC.
- Each factory operates independently — multi-factory orchestration is out of scope for this PoC, though the design should not preclude future multi-factory support.
- Standard cycle times for workstations will be configured as reference data for the 150% alert threshold.
- Quality inspection checklists (including safety-critical item classification) are pre-configured per vehicle model.
- Reporting and BI capabilities are out of scope.
