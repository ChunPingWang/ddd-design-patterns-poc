package com.automfg.manufacturing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AssemblyStepId(UUID value) {
    public AssemblyStepId {
        Objects.requireNonNull(value, "AssemblyStepId must not be null");
    }
}
