package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inspection_items")
public class InspectionItemJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspectionJpaEntity inspection;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "is_safety_related", nullable = false)
    private boolean safetyRelated;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected InspectionItemJpaEntity() {
        // JPA requires a no-arg constructor
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QualityInspectionJpaEntity getInspection() {
        return inspection;
    }

    public void setInspection(QualityInspectionJpaEntity inspection) {
        this.inspection = inspection;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSafetyRelated() {
        return safetyRelated;
    }

    public void setSafetyRelated(boolean safetyRelated) {
        this.safetyRelated = safetyRelated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
