package com.automfg.vehicleconfig.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "color_options")
public class ColorOptionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_config_id", nullable = false)
    private UUID vehicleConfigurationId;

    @Column(name = "color_code", nullable = false, length = 30)
    private String colorCode;

    @Column(name = "color_name", nullable = false, length = 50)
    private String colorName;

    protected ColorOptionJpaEntity() {
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

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
}
