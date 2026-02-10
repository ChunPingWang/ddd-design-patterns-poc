package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record MaterialBatchId(String value) {
    public MaterialBatchId {
        Objects.requireNonNull(value, "MaterialBatchId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MaterialBatchId must not be blank");
        }
    }
}
