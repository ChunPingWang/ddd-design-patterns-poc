package com.automfg.manufacturing.domain.model;

import com.automfg.manufacturing.domain.event.ReworkCompletedEvent;
import com.automfg.manufacturing.domain.event.ReworkOrderCreatedEvent;
import com.automfg.shared.domain.AggregateRoot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a rework order triggered by a failed quality inspection.
 */
public class ReworkOrder extends AggregateRoot {

    private final UUID id;
    private final ProductionOrderId productionOrderId;
    private final QualityInspectionId inspectionId;
    private ReworkStatus status;
    private final List<String> failedItemDescriptions;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private ReworkOrder(UUID id, ProductionOrderId productionOrderId,
                        QualityInspectionId inspectionId, ReworkStatus status,
                        List<String> failedItemDescriptions, LocalDateTime createdAt,
                        LocalDateTime completedAt) {
        this.id = Objects.requireNonNull(id, "Rework order ID must not be null");
        this.productionOrderId = Objects.requireNonNull(productionOrderId, "ProductionOrderId must not be null");
        this.inspectionId = Objects.requireNonNull(inspectionId, "QualityInspectionId must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.failedItemDescriptions = List.copyOf(Objects.requireNonNull(failedItemDescriptions,
            "Failed item descriptions must not be null"));
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.completedAt = completedAt;
    }

    /**
     * Factory method: Creates a new ReworkOrder.
     * Status starts as CREATED. Registers ReworkOrderCreatedEvent.
     */
    public static ReworkOrder create(UUID id, ProductionOrderId productionOrderId,
                                      QualityInspectionId inspectionId,
                                      List<String> failedItemDescriptions) {
        ReworkOrder order = new ReworkOrder(
            id, productionOrderId, inspectionId, ReworkStatus.CREATED,
            failedItemDescriptions, LocalDateTime.now(), null);

        order.registerEvent(new ReworkOrderCreatedEvent(
            id, productionOrderId.value(), inspectionId.value()));

        return order;
    }

    /**
     * Reconstitutes a ReworkOrder from persistence — no events registered.
     */
    public static ReworkOrder reconstitute(UUID id, ProductionOrderId productionOrderId,
                                            QualityInspectionId inspectionId, ReworkStatus status,
                                            List<String> failedItemDescriptions, LocalDateTime createdAt,
                                            LocalDateTime completedAt) {
        return new ReworkOrder(id, productionOrderId, inspectionId, status,
            failedItemDescriptions, createdAt, completedAt);
    }

    /**
     * Completes the rework order.
     * Transitions status to COMPLETED and sets completedAt timestamp.
     * Registers ReworkCompletedEvent.
     */
    public void complete() {
        if (this.status == ReworkStatus.COMPLETED) {
            throw new IllegalStateException("Rework order is already completed");
        }
        this.status = ReworkStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        registerEvent(new ReworkCompletedEvent(id, productionOrderId.value()));
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public ProductionOrderId getProductionOrderId() {
        return productionOrderId;
    }

    public QualityInspectionId getInspectionId() {
        return inspectionId;
    }

    public ReworkStatus getStatus() {
        return status;
    }

    public List<String> getFailedItemDescriptions() {
        return failedItemDescriptions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
