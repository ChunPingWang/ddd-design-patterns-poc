package com.automfg.manufacturing.domain.model;

import com.automfg.manufacturing.domain.event.AssemblyCompletedEvent;
import com.automfg.manufacturing.domain.event.AssemblyOvertimeAlertEvent;
import com.automfg.manufacturing.domain.event.MaterialShortageEvent;
import com.automfg.manufacturing.domain.event.ProductionOrderScheduledEvent;
import com.automfg.manufacturing.domain.event.ProductionStartedEvent;
import com.automfg.shared.domain.AggregateRoot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProductionOrder extends AggregateRoot {

    private final ProductionOrderId id;
    private final ProductionOrderNumber orderNumber;
    private final UUID sourceOrderId;
    private final VIN vin;
    private ProductionOrderStatus status;
    private BomSnapshot bomSnapshot;
    private AssemblyProcess assemblyProcess;
    private Integer currentStationSequence;
    private LocalDateTime scheduledStartDate;
    private final LocalDateTime createdAt;

    // Private constructor used by factory and reconstitute
    private ProductionOrder(ProductionOrderId id, ProductionOrderNumber orderNumber,
                            UUID sourceOrderId, VIN vin, ProductionOrderStatus status,
                            BomSnapshot bomSnapshot, AssemblyProcess assemblyProcess,
                            Integer currentStationSequence, LocalDateTime scheduledStartDate,
                            LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "ProductionOrderId must not be null");
        this.orderNumber = Objects.requireNonNull(orderNumber, "ProductionOrderNumber must not be null");
        this.sourceOrderId = Objects.requireNonNull(sourceOrderId, "Source order ID must not be null");
        this.vin = Objects.requireNonNull(vin, "VIN must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.bomSnapshot = bomSnapshot;
        this.assemblyProcess = assemblyProcess;
        this.currentStationSequence = currentStationSequence;
        this.scheduledStartDate = scheduledStartDate;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    /**
     * Factory method: Creates a new ProductionOrder.
     * If BOM is fully available -> status = SCHEDULED, registers ProductionOrderScheduledEvent.
     * Otherwise -> status = MATERIAL_PENDING, registers MaterialShortageEvent.
     */
    public static ProductionOrder create(ProductionOrderId id, ProductionOrderNumber orderNumber,
                                         UUID sourceOrderId, VIN vin, BomSnapshot bomSnapshot,
                                         List<AssemblyStepTemplate> assemblyStepTemplates) {
        Objects.requireNonNull(bomSnapshot, "BomSnapshot must not be null");
        Objects.requireNonNull(assemblyStepTemplates, "Assembly step templates must not be null");
        if (assemblyStepTemplates.isEmpty()) {
            throw new IllegalArgumentException("Assembly step templates must not be empty");
        }

        AssemblyProcess assemblyProcess = new AssemblyProcess(
            new AssemblyProcessId(UUID.randomUUID()), assemblyStepTemplates);

        ProductionOrderStatus initialStatus;
        if (bomSnapshot.isFullyAvailable()) {
            initialStatus = ProductionOrderStatus.SCHEDULED;
        } else {
            initialStatus = ProductionOrderStatus.MATERIAL_PENDING;
        }

        ProductionOrder order = new ProductionOrder(
            id, orderNumber, sourceOrderId, vin, initialStatus,
            bomSnapshot, assemblyProcess, null, null, LocalDateTime.now());

        if (initialStatus == ProductionOrderStatus.SCHEDULED) {
            order.registerEvent(new ProductionOrderScheduledEvent(
                id.value(), orderNumber.value(), sourceOrderId, vin.value()));
        } else {
            List<String> missingParts = bomSnapshot.getMissingMaterials().stream()
                .map(BomLineItem::partNumber)
                .toList();
            order.registerEvent(new MaterialShortageEvent(
                id.value(), sourceOrderId, missingParts));
        }

        return order;
    }

    /**
     * Reconstitutes a ProductionOrder from persistence — no events registered.
     */
    public static ProductionOrder reconstitute(ProductionOrderId id, ProductionOrderNumber orderNumber,
                                               UUID sourceOrderId, VIN vin, ProductionOrderStatus status,
                                               BomSnapshot bomSnapshot, AssemblyProcess assemblyProcess,
                                               Integer currentStationSequence, LocalDateTime scheduledStartDate,
                                               LocalDateTime createdAt) {
        return new ProductionOrder(id, orderNumber, sourceOrderId, vin, status,
            bomSnapshot, assemblyProcess, currentStationSequence, scheduledStartDate, createdAt);
    }

    /**
     * Starts production for this order.
     * Validates status == SCHEDULED, transitions to IN_PRODUCTION.
     */
    public void startProduction(String operatorId, String workstationCode) {
        if (this.status != ProductionOrderStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Cannot start production: order status is " + this.status + ", expected SCHEDULED");
        }
        Objects.requireNonNull(operatorId, "Operator ID must not be null");
        Objects.requireNonNull(workstationCode, "Workstation code must not be null");

        this.status = ProductionOrderStatus.IN_PRODUCTION;
        this.currentStationSequence = 1;
        this.assemblyProcess.start();

        registerEvent(new ProductionStartedEvent(
            id.value(), vin.value(), operatorId));
    }

    /**
     * Completes an assembly step within the production order.
     * BR-09: Registers AssemblyOvertimeAlertEvent if actual > standard * 1.5.
     * Advances station sequence and completes assembly when appropriate.
     */
    public AssemblyStepResult completeAssemblyStep(AssemblyStepId stepId, String operatorId,
                                                    String materialBatchId, int actualMinutes) {
        if (this.status != ProductionOrderStatus.IN_PRODUCTION) {
            throw new IllegalStateException(
                "Cannot complete assembly step: order status is " + this.status + ", expected IN_PRODUCTION");
        }

        // Get the step's task description and standard time before completion for event data
        AssemblyStep step = assemblyProcess.getStep(stepId);
        String stepDescription = step.getTaskDescription();
        int standardMinutes = step.getStandardTimeMinutes();

        AssemblyStepResult result = assemblyProcess.completeStep(stepId, operatorId, materialBatchId, actualMinutes);

        // BR-09: Overtime alert
        if (result.overtimeAlert()) {
            registerEvent(new AssemblyOvertimeAlertEvent(
                id.value(), stepDescription, standardMinutes, actualMinutes));
        }

        // Advance station sequence when all steps at current station are completed
        if (result.stationCompleted() && !result.assemblyCompleted()) {
            this.currentStationSequence = this.currentStationSequence + 1;
        }

        // All steps completed -> ASSEMBLY_COMPLETED
        if (result.assemblyCompleted()) {
            this.status = ProductionOrderStatus.ASSEMBLY_COMPLETED;
            registerEvent(new AssemblyCompletedEvent(id.value(), vin.value()));
        }

        return result;
    }

    /**
     * Marks inspection as passed. ASSEMBLY_COMPLETED -> INSPECTION_PASSED.
     */
    public void markInspectionPassed() {
        if (this.status != ProductionOrderStatus.ASSEMBLY_COMPLETED) {
            throw new IllegalStateException(
                "Cannot mark inspection passed: order status is " + this.status + ", expected ASSEMBLY_COMPLETED");
        }
        this.status = ProductionOrderStatus.INSPECTION_PASSED;
    }

    /**
     * Marks inspection as failed. ASSEMBLY_COMPLETED -> INSPECTION_FAILED.
     */
    public void markInspectionFailed() {
        if (this.status != ProductionOrderStatus.ASSEMBLY_COMPLETED) {
            throw new IllegalStateException(
                "Cannot mark inspection failed: order status is " + this.status + ", expected ASSEMBLY_COMPLETED");
        }
        this.status = ProductionOrderStatus.INSPECTION_FAILED;
    }

    /**
     * Starts rework. INSPECTION_FAILED -> REWORK_IN_PROGRESS.
     */
    public void startRework() {
        if (this.status != ProductionOrderStatus.INSPECTION_FAILED) {
            throw new IllegalStateException(
                "Cannot start rework: order status is " + this.status + ", expected INSPECTION_FAILED");
        }
        this.status = ProductionOrderStatus.REWORK_IN_PROGRESS;
    }

    /**
     * Completes rework. REWORK_IN_PROGRESS -> ASSEMBLY_COMPLETED.
     */
    public void completeRework() {
        if (this.status != ProductionOrderStatus.REWORK_IN_PROGRESS) {
            throw new IllegalStateException(
                "Cannot complete rework: order status is " + this.status + ", expected REWORK_IN_PROGRESS");
        }
        this.status = ProductionOrderStatus.ASSEMBLY_COMPLETED;
    }

    /**
     * Returns true if the order can be modified (MATERIAL_PENDING or SCHEDULED).
     */
    public boolean isModifiable() {
        return this.status == ProductionOrderStatus.MATERIAL_PENDING
            || this.status == ProductionOrderStatus.SCHEDULED;
    }

    // Getters

    public ProductionOrderId getId() {
        return id;
    }

    public ProductionOrderNumber getOrderNumber() {
        return orderNumber;
    }

    public UUID getSourceOrderId() {
        return sourceOrderId;
    }

    public VIN getVin() {
        return vin;
    }

    public ProductionOrderStatus getStatus() {
        return status;
    }

    public BomSnapshot getBomSnapshot() {
        return bomSnapshot;
    }

    public AssemblyProcess getAssemblyProcess() {
        return assemblyProcess;
    }

    public Integer getCurrentStationSequence() {
        return currentStationSequence;
    }

    public LocalDateTime getScheduledStartDate() {
        return scheduledStartDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
