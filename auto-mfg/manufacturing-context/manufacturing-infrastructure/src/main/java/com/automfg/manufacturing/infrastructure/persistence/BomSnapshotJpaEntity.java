package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bom_snapshots")
public class BomSnapshotJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false, unique = true)
    private ProductionOrderJpaEntity productionOrder;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    @OneToMany(mappedBy = "bomSnapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BomLineItemJpaEntity> lineItems = new ArrayList<>();

    protected BomSnapshotJpaEntity() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionOrderJpaEntity getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrderJpaEntity productionOrder) {
        this.productionOrder = productionOrder;
    }

    public LocalDateTime getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDateTime snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public List<BomLineItemJpaEntity> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<BomLineItemJpaEntity> lineItems) {
        this.lineItems = lineItems;
    }
}
