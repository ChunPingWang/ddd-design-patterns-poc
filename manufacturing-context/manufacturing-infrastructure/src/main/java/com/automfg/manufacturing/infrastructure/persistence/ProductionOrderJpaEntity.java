package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_orders")
public class ProductionOrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 25)
    private String orderNumber;

    @Column(name = "source_order_id", nullable = false, unique = true)
    private UUID sourceOrderId;

    @Column(name = "vin", nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "current_station_sequence")
    private Integer currentStationSequence;

    @Column(name = "scheduled_start_date")
    private LocalDateTime scheduledStartDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BomSnapshotJpaEntity bomSnapshot;

    @OneToOne(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private AssemblyProcessJpaEntity assemblyProcess;

    protected ProductionOrderJpaEntity() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(UUID sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentStationSequence() {
        return currentStationSequence;
    }

    public void setCurrentStationSequence(Integer currentStationSequence) {
        this.currentStationSequence = currentStationSequence;
    }

    public LocalDateTime getScheduledStartDate() {
        return scheduledStartDate;
    }

    public void setScheduledStartDate(LocalDateTime scheduledStartDate) {
        this.scheduledStartDate = scheduledStartDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BomSnapshotJpaEntity getBomSnapshot() {
        return bomSnapshot;
    }

    public void setBomSnapshot(BomSnapshotJpaEntity bomSnapshot) {
        this.bomSnapshot = bomSnapshot;
    }

    public AssemblyProcessJpaEntity getAssemblyProcess() {
        return assemblyProcess;
    }

    public void setAssemblyProcess(AssemblyProcessJpaEntity assemblyProcess) {
        this.assemblyProcess = assemblyProcess;
    }
}
