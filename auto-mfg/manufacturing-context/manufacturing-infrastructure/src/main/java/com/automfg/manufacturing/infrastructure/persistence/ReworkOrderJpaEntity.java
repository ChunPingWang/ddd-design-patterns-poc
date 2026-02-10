package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rework_orders")
public class ReworkOrderJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "production_order_id", nullable = false)
    private UUID productionOrderId;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failed_items", columnDefinition = "TEXT")
    private String failedItems;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected ReworkOrderJpaEntity() {
        // JPA requires a no-arg constructor
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(UUID productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(UUID inspectionId) {
        this.inspectionId = inspectionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(String failedItems) {
        this.failedItems = failedItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
