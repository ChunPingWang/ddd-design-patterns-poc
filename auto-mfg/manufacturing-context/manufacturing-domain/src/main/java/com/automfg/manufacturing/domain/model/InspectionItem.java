package com.automfg.manufacturing.domain.model;

import java.util.Objects;

/**
 * Entity within the QualityInspection aggregate.
 * Represents a single inspection checklist item.
 * Once a result is recorded, the item becomes immutable.
 */
public class InspectionItem {

    private final InspectionItemId id;
    private final String description;
    private final boolean safetyRelated;
    private InspectionItemStatus status;
    private String notes;

    public InspectionItem(InspectionItemId id, String description, boolean safetyRelated) {
        this.id = Objects.requireNonNull(id, "InspectionItemId must not be null");
        this.description = Objects.requireNonNull(description, "Description must not be null");
        this.safetyRelated = safetyRelated;
        this.status = InspectionItemStatus.PENDING;
        this.notes = null;
    }

    /**
     * Reconstitutes an InspectionItem from persistence — no validation enforced.
     */
    public static InspectionItem reconstitute(InspectionItemId id, String description,
                                               boolean safetyRelated, InspectionItemStatus status,
                                               String notes) {
        InspectionItem item = new InspectionItem(id, description, safetyRelated);
        item.status = status;
        item.notes = notes;
        return item;
    }

    /**
     * Records the result for this inspection item.
     * Can only be called once — item is immutable after recording.
     *
     * @param status the inspection result status (PASSED, FAILED, or CONDITIONAL)
     * @param notes optional notes about the inspection finding
     */
    public void recordResult(InspectionItemStatus status, String notes) {
        if (this.status != InspectionItemStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot record result: item '" + description + "' already has status " + this.status);
        }
        Objects.requireNonNull(status, "Status must not be null");
        if (status == InspectionItemStatus.PENDING) {
            throw new IllegalArgumentException("Cannot record PENDING as a result");
        }
        this.status = status;
        this.notes = notes;
    }

    public boolean isSafetyRelated() {
        return safetyRelated;
    }

    public boolean isFailed() {
        return status == InspectionItemStatus.FAILED;
    }

    public boolean isConditional() {
        return status == InspectionItemStatus.CONDITIONAL;
    }

    public boolean isPending() {
        return status == InspectionItemStatus.PENDING;
    }

    public InspectionItemId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public InspectionItemStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
}
