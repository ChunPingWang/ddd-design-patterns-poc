# Data Model: AutoMFG DDD PoC

**Feature**: 001-auto-mfg-ddd-poc
**Date**: 2026-02-11

## Overview

This data model follows DDD tactical design patterns. Domain entities (aggregates, entities, value objects) are defined separately from persistence entities (JPA). The mapping between them is handled by dedicated mapper classes in the infrastructure layer.

---

## Order Management Context (`order_ctx` schema)

### Order (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Internal identity |
| order_number | String | Unique, Not Null | Format: ORD-{YYYYMM}-{SEQ} |
| dealer_id | String | Not Null | FK reference to Dealer |
| vehicle_model_code | String | Not Null | References Vehicle Configuration context |
| color_code | String | Not Null | |
| option_package_codes | List\<String\> | | Selected option packages |
| status | Enum | Not Null | PLACED, SCHEDULED, IN_PRODUCTION, COMPLETED, CANCELLED |
| estimated_delivery_date | LocalDate | Not Null | >= order_date + 45 days |
| price_quote | BigDecimal | Not Null | Calculated from model + options |
| change_count | int | Not Null, Default 0 | Max 3 |
| order_date | LocalDateTime | Not Null | |
| created_at | LocalDateTime | Not Null | |
| updated_at | LocalDateTime | Not Null | |

**Business Rules**:
- BR-01: Max 50 pending orders per dealer per vehicle model (enforced via repository query + aggregate validation)
- BR-02: Option compatibility validated via Vehicle Configuration context (ACL)
- BR-03: estimated_delivery_date >= order_date + 45 days
- BR-14: Model change = cancel + new order
- BR-15: change_count <= 3

**State Transitions**:
```
PLACED → SCHEDULED (when production order is scheduled)
PLACED → CANCELLED (dealer cancels or model change)
SCHEDULED → IN_PRODUCTION (production starts)
IN_PRODUCTION → COMPLETED (vehicle passes inspection)
Any pre-production → CANCELLED (dealer cancels)
```

### Dealer (Value Object in Order context)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| dealer_id | String | Not Null | Unique identifier |
| dealer_name | String | Not Null | |

---

## Manufacturing Management Context (`mfg_ctx` schema)

### ProductionOrder (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Internal identity |
| order_number | ProductionOrderNumber | Unique, Not Null | Format: PO-{FACTORY}-{YYYYMM}-{SEQ} |
| source_order_id | UUID | Not Null, Unique | 1:1 with customer Order (idempotency key) |
| vin | VIN | Unique, Not Null | 17-char, assigned at creation |
| status | Enum | Not Null | MATERIAL_PENDING, SCHEDULED, IN_PRODUCTION, ASSEMBLY_COMPLETED, INSPECTION_PASSED, INSPECTION_FAILED, REWORK_IN_PROGRESS |
| current_station_sequence | int | Nullable | Current workstation sequence number |
| scheduled_start_date | LocalDateTime | Nullable | |
| created_at | LocalDateTime | Not Null | |

**State Transitions**:
```
                        ┌─── MATERIAL_PENDING ←── (materials insufficient)
                        │          │
                        │          ▼ (materials become available)
(create) ──────────────►├─── SCHEDULED
                                   │
                                   ▼ (operator scans at first station)
                              IN_PRODUCTION
                                   │
                                   ▼ (all stations completed)
                            ASSEMBLY_COMPLETED
                               │          │
                               ▼          ▼
                    INSPECTION_PASSED  INSPECTION_FAILED
                                          │
                                          ▼
                                   REWORK_IN_PROGRESS
                                          │
                                          ▼ (rework done → re-inspect)
                                   ASSEMBLY_COMPLETED
```

### BomSnapshot (Value Object, embedded in ProductionOrder)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| production_order_id | UUID | FK | Parent reference |
| snapshot_date | LocalDateTime | Not Null | When BOM was captured |

### BomLineItem (Value Object, child of BomSnapshot)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| bom_snapshot_id | UUID | FK | Parent reference |
| part_number | String | Not Null | |
| part_description | String | Not Null | |
| quantity_required | int | Not Null, > 0 | |
| unit_of_measure | String | Not Null | |
| is_available | boolean | Not Null | Availability at snapshot time |

### AssemblyProcess (Entity, owned by ProductionOrder)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| production_order_id | UUID | FK, Unique | 1:1 with ProductionOrder |
| status | Enum | Not Null | NOT_STARTED, IN_PROGRESS, COMPLETED |

### AssemblyStep (Entity, owned by AssemblyProcess) — IMMUTABLE after completion

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| assembly_process_id | UUID | FK | Parent reference |
| work_station_code | String | Not Null | |
| work_station_sequence | int | Not Null, > 0 | Enforces ordering |
| task_description | String | Not Null | |
| standard_time_minutes | int | Not Null, > 0 | |
| status | Enum | Not Null | PENDING, IN_PROGRESS, COMPLETED |
| operator_id | String | Nullable | Set on completion |
| material_batch_id | String | Not Null on completion | Required for traceability (BR-08) |
| actual_time_minutes | int | Nullable | Set on completion |
| completed_at | LocalDateTime | Nullable | Set on completion |
| corrects_record_id | UUID | Nullable | FK to original record if this is a correction |

**Immutability Rule**: Once status = COMPLETED, no fields may be updated. Corrections are new records with `corrects_record_id` pointing to the original.

### QualityInspection (Aggregate Root) — IMMUTABLE after review

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| production_order_id | UUID | Not Null | FK reference (by ID, not object) |
| vin | VIN | Not Null | |
| result | Enum | Nullable | PASSED, CONDITIONAL_PASS, FAILED |
| inspector_id | String | Not Null | Who performed the inspection |
| reviewer_id | String | Nullable | Must differ from inspector (BR-12) |
| inspected_at | LocalDateTime | Nullable | |
| reviewed_at | LocalDateTime | Nullable | |
| created_at | LocalDateTime | Not Null | |
| corrects_record_id | UUID | Nullable | FK to original if this is a re-inspection |

### InspectionItem (Entity, owned by QualityInspection) — IMMUTABLE after recording

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| inspection_id | UUID | FK | Parent reference |
| description | String | Not Null | From checklist template |
| is_safety_related | boolean | Not Null | Safety-critical flag |
| status | Enum | Not Null | PENDING, PASSED, FAILED, CONDITIONAL |
| notes | String | Nullable | Inspector notes |

### ReworkOrder (Entity, referenced by QualityInspection)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| production_order_id | UUID | Not Null | |
| inspection_id | UUID | Not Null | FK to failed inspection |
| status | Enum | Not Null | CREATED, IN_PROGRESS, COMPLETED |
| failed_items | List\<UUID\> | Not Null | IDs of failed InspectionItems |
| created_at | LocalDateTime | Not Null | |
| completed_at | LocalDateTime | Nullable | |

---

## Vehicle Configuration Context (`vehicle_ctx` schema)

### VehicleConfiguration (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| model_code | String | Unique, Not Null | e.g., "MODEL-X-SEDAN" |
| model_name | String | Not Null | Display name |
| available_colors | List\<ColorOption\> | Not Null | |
| is_active | boolean | Not Null | Whether orderable |

### OptionPackage (Entity, owned by VehicleConfiguration)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| vehicle_config_id | UUID | FK | |
| package_code | String | Not Null | e.g., "LUXURY-INTERIOR" |
| package_name | String | Not Null | |
| base_price | BigDecimal | Not Null | |

### CompatibilityRule (Value Object)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| vehicle_config_id | UUID | FK | |
| option_code_a | String | Not Null | |
| option_code_b | String | Not Null | |
| rule_type | Enum | Not Null | INCOMPATIBLE, REQUIRES |
| description | String | | Human-readable explanation |

### InspectionChecklist (Reference Data, linked to VehicleConfiguration)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| model_code | String | Not Null | |
| item_description | String | Not Null | |
| is_safety_related | boolean | Not Null | |
| display_order | int | Not Null | |

---

## Infrastructure Tables

### ProcessedEvent (Idempotency tracking)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| event_id | UUID | PK | Domain event ID |
| event_type | String | Not Null | |
| processed_at | LocalDateTime | Not Null | |
| consumer_name | String | Not Null | Which consumer processed it |

### DomainEventOutbox (Transactional Outbox)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | |
| aggregate_type | String | Not Null | e.g., "ProductionOrder" |
| aggregate_id | UUID | Not Null | |
| event_type | String | Not Null | |
| payload | JSONB | Not Null | Serialized event |
| created_at | LocalDateTime | Not Null | |
| published_at | LocalDateTime | Nullable | Null until relayed to Kafka |

---

## Value Objects Summary

| Value Object | Context | Format / Rules |
|--------------|---------|----------------|
| VIN | Manufacturing | 17 chars, [A-HJ-NPR-Z0-9], ISO 3779 |
| ProductionOrderNumber | Manufacturing | PO-{XX}-{YYYYMM}-{NNNNN} |
| MaterialBatchId | Manufacturing | Non-blank string |
| Duration | Manufacturing | Minutes (int >= 0) |
| WorkStationId | Manufacturing | Code (string) + Sequence (int >= 1) |
| OrderNumber | Order | ORD-{YYYYMM}-{NNNNN} |
| Money | Shared | BigDecimal amount + currency code |
| ColorOption | Vehicle Config | Code + name + hex value |
