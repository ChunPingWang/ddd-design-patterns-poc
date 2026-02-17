package com.automfg.manufacturing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProductionOrderId(UUID value) {
    public ProductionOrderId {
        Objects.requireNonNull(value, "ProductionOrderId must not be null");
    }
}
