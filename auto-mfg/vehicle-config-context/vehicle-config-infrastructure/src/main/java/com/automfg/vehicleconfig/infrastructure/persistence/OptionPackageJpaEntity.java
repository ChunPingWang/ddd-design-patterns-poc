package com.automfg.vehicleconfig.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "option_packages")
public class OptionPackageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_config_id", nullable = false)
    private UUID vehicleConfigurationId;

    @Column(name = "package_code", nullable = false, length = 50)
    private String packageCode;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    protected OptionPackageJpaEntity() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVehicleConfigurationId() {
        return vehicleConfigurationId;
    }

    public void setVehicleConfigurationId(UUID vehicleConfigurationId) {
        this.vehicleConfigurationId = vehicleConfigurationId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
}
