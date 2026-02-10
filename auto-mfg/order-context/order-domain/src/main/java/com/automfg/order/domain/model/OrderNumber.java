package com.automfg.order.domain.model;

import java.util.Objects;

public record OrderNumber(String value) {
    private static final String FORMAT_PATTERN = "ORD-\\d{6}-\\d{5}";

    public OrderNumber {
        Objects.requireNonNull(value, "OrderNumber must not be null");
        if (!value.matches(FORMAT_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid OrderNumber format: " + value + ". Expected format: ORD-YYYYMM-NNNNN");
        }
    }
}
