package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inspection_checklists")
public class InspectionChecklistJpaEntity {

    @Id
    @Column(name = "id")
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
        // JPA requires a no-arg constructor
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
