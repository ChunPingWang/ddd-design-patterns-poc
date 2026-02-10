package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record AssemblyStepTemplate(
    String workStationCode,
    int workStationSequence,
    String taskDescription,
    int standardTimeMinutes
) {
    public AssemblyStepTemplate {
        Objects.requireNonNull(workStationCode, "WorkStation code must not be null");
        if (workStationSequence < 1) {
            throw new IllegalArgumentException("WorkStation sequence must be positive");
        }
        Objects.requireNonNull(taskDescription, "Task description must not be null");
        if (standardTimeMinutes <= 0) {
            throw new IllegalArgumentException("Standard time must be positive");
        }
    }
}
