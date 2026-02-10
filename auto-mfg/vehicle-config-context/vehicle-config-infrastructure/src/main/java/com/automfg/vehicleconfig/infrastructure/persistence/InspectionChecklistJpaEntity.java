package com.automfg.vehicleconfig.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "inspection_checklists")
public class InspectionChecklistJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "model_code", nullable = false, length = 50)
    private String modelCode;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "is_safety_related", nullable = false)
    private boolean safetyRelated;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected InspectionChecklistJpaEntity() {
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

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public boolean isSafetyRelated() {
        return safetyRelated;
    }

    public void setSafetyRelated(boolean safetyRelated) {
        this.safetyRelated = safetyRelated;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
