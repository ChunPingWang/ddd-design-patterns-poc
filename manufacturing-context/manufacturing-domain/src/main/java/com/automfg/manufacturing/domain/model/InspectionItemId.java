package com.automfg.manufacturing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record InspectionItemId(UUID value) {
    public InspectionItemId {
        Objects.requireNonNull(value, "InspectionItemId must not be null");
    }
}
