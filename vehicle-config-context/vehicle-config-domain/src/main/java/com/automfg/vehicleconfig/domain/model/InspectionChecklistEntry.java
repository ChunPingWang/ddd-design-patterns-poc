package com.automfg.vehicleconfig.domain.model;

import java.util.Objects;

public record InspectionChecklistEntry(
    String description,
    boolean safetyRelated,
    int displayOrder
) {
    public InspectionChecklistEntry {
        Objects.requireNonNull(description, "Description must not be null");
        if (displayOrder < 1) {
            throw new IllegalArgumentException("Display order must be positive");
        }
    }
}
