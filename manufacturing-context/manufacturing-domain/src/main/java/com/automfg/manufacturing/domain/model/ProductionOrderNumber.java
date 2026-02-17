package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record ProductionOrderNumber(String value) {
    public ProductionOrderNumber {
        Objects.requireNonNull(value, "ProductionOrderNumber must not be null");
        if (!value.matches("PO-[A-Z]{2}-\\d{6}-\\d{5}")) {
            throw new IllegalArgumentException("Invalid ProductionOrderNumber format: " + value);
        }
    }

    public String factoryCode() {
        return value.substring(3, 5);
    }
}
