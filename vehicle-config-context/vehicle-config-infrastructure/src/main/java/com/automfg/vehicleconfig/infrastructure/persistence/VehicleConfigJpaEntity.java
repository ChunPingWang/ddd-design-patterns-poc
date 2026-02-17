package com.automfg.vehicleconfig.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vehicle_configurations")
public class VehicleConfigJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "model_code", nullable = false, unique = true, length = 50)
    private String modelCode;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected VehicleConfigJpaEntity() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns null since the vehicle_configurations table does not have a base_price column.
     * Price calculation is based solely on option packages.
     */
    public BigDecimal getBasePrice() {
        return BigDecimal.ZERO;
    }
}
