package com.automfg.manufacturing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record QualityInspectionId(UUID value) {
    public QualityInspectionId {
        Objects.requireNonNull(value, "QualityInspectionId must not be null");
    }
}
