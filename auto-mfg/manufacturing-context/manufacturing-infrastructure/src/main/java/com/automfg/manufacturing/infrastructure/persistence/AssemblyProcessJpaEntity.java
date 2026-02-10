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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assembly_processes")
public class AssemblyProcessJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false, unique = true)
    private ProductionOrderJpaEntity productionOrder;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "assemblyProcess", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<AssemblyStepJpaEntity> steps = new ArrayList<>();

    protected AssemblyProcessJpaEntity() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AssemblyStepJpaEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<AssemblyStepJpaEntity> steps) {
        this.steps = steps;
    }
}
