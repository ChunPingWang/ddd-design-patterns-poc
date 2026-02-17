package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record WorkStationId(String code, int sequence) {
    public WorkStationId {
        Objects.requireNonNull(code, "WorkStation code must not be null");
        if (sequence < 1) {
            throw new IllegalArgumentException("WorkStation sequence must be positive: " + sequence);
        }
    }

    public boolean isNextOf(WorkStationId previous) {
        return this.sequence == previous.sequence + 1;
    }
}
