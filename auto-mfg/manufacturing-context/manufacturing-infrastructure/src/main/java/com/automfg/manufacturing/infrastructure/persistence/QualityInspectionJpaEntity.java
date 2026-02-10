package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quality_inspections")
public class QualityInspectionJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "production_order_id", nullable = false)
    private UUID productionOrderId;

    @Column(name = "vin", nullable = false, length = 17)
    private String vin;

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "inspector_id", nullable = false, length = 50)
    private String inspectorId;

    @Column(name = "reviewer_id", length = 50)
    private String reviewerId;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "corrects_record_id")
    private UUID correctsRecordId;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InspectionItemJpaEntity> items = new ArrayList<>();

    protected QualityInspectionJpaEntity() {
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

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getInspectorId() {
        return inspectorId;
    }

    public void setInspectorId(String inspectorId) {
        this.inspectorId = inspectorId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(LocalDateTime inspectedAt) {
        this.inspectedAt = inspectedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCorrectsRecordId() {
        return correctsRecordId;
    }

    public void setCorrectsRecordId(UUID correctsRecordId) {
        this.correctsRecordId = correctsRecordId;
    }

    public List<InspectionItemJpaEntity> getItems() {
        return items;
    }

    public void setItems(List<InspectionItemJpaEntity> items) {
        this.items = items;
    }
}
