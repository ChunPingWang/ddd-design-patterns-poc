package com.automfg.manufacturing.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class AssemblyStep {

    private final AssemblyStepId id;
    private final WorkStationId workStation;
    private final String taskDescription;
    private final int standardTimeMinutes;
    private AssemblyStepStatus status;
    private String operatorId;
    private MaterialBatchId materialBatchId;
    private Integer actualTimeMinutes;
    private LocalDateTime completedAt;

    public AssemblyStep(AssemblyStepId id, WorkStationId workStation,
                        String taskDescription, int standardTimeMinutes) {
        this.id = Objects.requireNonNull(id, "AssemblyStepId must not be null");
        this.workStation = Objects.requireNonNull(workStation, "WorkStation must not be null");
        this.taskDescription = Objects.requireNonNull(taskDescription, "Task description must not be null");
        if (standardTimeMinutes <= 0) {
            throw new IllegalArgumentException("Standard time must be positive");
        }
        this.standardTimeMinutes = standardTimeMinutes;
        this.status = AssemblyStepStatus.PENDING;
    }

    /**
     * Creates an AssemblyStep from a template.
     */
    public static AssemblyStep fromTemplate(AssemblyStepTemplate template) {
        return new AssemblyStep(
            new AssemblyStepId(UUID.randomUUID()),
            new WorkStationId(template.workStationCode(), template.workStationSequence()),
            template.taskDescription(),
            template.standardTimeMinutes()
        );
    }

    /**
     * Reconstitutes an AssemblyStep from persistence (no validation, no events).
     */
    public static AssemblyStep reconstitute(AssemblyStepId id, WorkStationId workStation,
                                            String taskDescription, int standardTimeMinutes,
                                            AssemblyStepStatus status, String operatorId,
                                            MaterialBatchId materialBatchId, Integer actualTimeMinutes,
                                            LocalDateTime completedAt) {
        AssemblyStep step = new AssemblyStep(id, workStation, taskDescription, standardTimeMinutes);
        step.status = status;
        step.operatorId = operatorId;
        step.materialBatchId = materialBatchId;
        step.actualTimeMinutes = actualTimeMinutes;
        step.completedAt = completedAt;
        return step;
    }

    /**
     * Completes this assembly step. Steps are IMMUTABLE after completion.
     * BR-08: Material batch ID is required.
     */
    public void complete(String operatorId, String materialBatchId, int actualMinutes) {
        if (this.status == AssemblyStepStatus.COMPLETED) {
            throw new IllegalStateException(
                "Assembly step " + id.value() + " is already completed and cannot be modified");
        }
        if (this.status != AssemblyStepStatus.PENDING && this.status != AssemblyStepStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Assembly step " + id.value() + " is in invalid status: " + this.status);
        }
        Objects.requireNonNull(operatorId, "Operator ID must not be null");
        if (operatorId.isBlank()) {
            throw new IllegalArgumentException("Operator ID must not be blank");
        }
        // BR-08: Material batch ID is required for each assembly step
        if (materialBatchId == null || materialBatchId.isBlank()) {
            throw new IllegalArgumentException("Material batch ID is required for assembly step completion (BR-08)");
        }
        if (actualMinutes <= 0) {
            throw new IllegalArgumentException("Actual time must be positive");
        }

        this.operatorId = operatorId;
        this.materialBatchId = new MaterialBatchId(materialBatchId);
        this.actualTimeMinutes = actualMinutes;
        this.status = AssemblyStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * BR-09: Overtime when actual exceeds 150% of standard time.
     */
    public boolean isOvertime() {
        return actualTimeMinutes != null && actualTimeMinutes > standardTimeMinutes * 1.5;
    }

    public boolean isCompleted() {
        return status == AssemblyStepStatus.COMPLETED;
    }

    public AssemblyStepId getId() {
        return id;
    }

    public WorkStationId getWorkStation() {
        return workStation;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public int getStandardTimeMinutes() {
        return standardTimeMinutes;
    }

    public AssemblyStepStatus getStatus() {
        return status;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public MaterialBatchId getMaterialBatchId() {
        return materialBatchId;
    }

    public Integer getActualTimeMinutes() {
        return actualTimeMinutes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
