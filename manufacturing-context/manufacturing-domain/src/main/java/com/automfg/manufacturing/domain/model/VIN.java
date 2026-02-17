package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record VIN(String value) {
    public VIN {
        Objects.requireNonNull(value, "VIN must not be null");
        if (!value.matches("[A-HJ-NPR-Z0-9]{17}")) {
            throw new IllegalArgumentException(
                "Invalid VIN format: must be 17 alphanumeric chars excluding I, O, Q. Got: " + value);
        }
    }
}
