package com.automfg.vehicleconfig.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "compatibility_rules")
public class CompatibilityRuleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "vehicle_config_id", nullable = false)
    private UUID vehicleConfigurationId;

    @Column(name = "option_code_a", nullable = false, length = 50)
    private String optionCodeA;

    @Column(name = "option_code_b", nullable = false, length = 50)
    private String optionCodeB;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType;

    @Column(name = "description", length = 200)
    private String description;

    protected CompatibilityRuleJpaEntity() {
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

    public String getOptionCodeA() {
        return optionCodeA;
    }

    public void setOptionCodeA(String optionCodeA) {
        this.optionCodeA = optionCodeA;
    }

    public String getOptionCodeB() {
        return optionCodeB;
    }

    public void setOptionCodeB(String optionCodeB) {
        this.optionCodeB = optionCodeB;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
