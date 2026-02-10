package com.automfg.manufacturing.domain.model;

public record AssemblyStepResult(
    boolean overtimeAlert,
    boolean stationCompleted,
    boolean assemblyCompleted
) {
}
