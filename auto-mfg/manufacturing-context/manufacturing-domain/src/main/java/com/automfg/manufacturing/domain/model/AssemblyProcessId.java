package com.automfg.manufacturing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AssemblyProcessId(UUID value) {
    public AssemblyProcessId {
        Objects.requireNonNull(value, "AssemblyProcessId must not be null");
    }
}
