package com.automfg.manufacturing.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AssemblyProcess {

    private final AssemblyProcessId id;
    private AssemblyProcessStatus status;
    private final List<AssemblyStep> steps;

    public AssemblyProcess(AssemblyProcessId id, List<AssemblyStepTemplate> templates) {
        this.id = Objects.requireNonNull(id, "AssemblyProcessId must not be null");
        Objects.requireNonNull(templates, "Assembly step templates must not be null");
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("Assembly step templates must not be empty");
        }
        this.status = AssemblyProcessStatus.NOT_STARTED;
        this.steps = new ArrayList<>();
        for (AssemblyStepTemplate template : templates) {
            this.steps.add(AssemblyStep.fromTemplate(template));
        }
    }

    /**
     * Reconstitutes an AssemblyProcess from persistence (no validation, no events).
     */
    public static AssemblyProcess reconstitute(AssemblyProcessId id, AssemblyProcessStatus status,
                                               List<AssemblyStep> steps) {
        AssemblyProcess process = new AssemblyProcess(id, status, steps);
        return process;
    }

    private AssemblyProcess(AssemblyProcessId id, AssemblyProcessStatus status, List<AssemblyStep> steps) {
        this.id = Objects.requireNonNull(id);
        this.status = Objects.requireNonNull(status);
        this.steps = new ArrayList<>(Objects.requireNonNull(steps));
    }

    /**
     * Starts the assembly process.
     */
    public void start() {
        if (this.status != AssemblyProcessStatus.NOT_STARTED) {
            throw new IllegalStateException(
                "Cannot start assembly process: current status is " + this.status);
        }
        this.status = AssemblyProcessStatus.IN_PROGRESS;
    }

    /**
     * Completes a specific assembly step.
     * BR-07: Station sequence must be respected — all steps at station N must be completed before station N+1.
     */
    public AssemblyStepResult completeStep(AssemblyStepId stepId, String operatorId,
                                           String materialBatchId, int actualMinutes) {
        if (this.status != AssemblyProcessStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Cannot complete step: assembly process status is " + this.status);
        }

        AssemblyStep targetStep = findStep(stepId);
        int targetStationSequence = targetStep.getWorkStation().sequence();

        // BR-07: Validate that all steps at previous stations are completed
        validateStationSequence(targetStationSequence);

        // Complete the step
        targetStep.complete(operatorId, materialBatchId, actualMinutes);

        // Check if all steps at this station are completed
        boolean stationCompleted = areAllStepsAtStationCompleted(targetStationSequence);

        // Check if all steps are completed
        boolean allCompleted = areAllStepsCompleted();
        if (allCompleted) {
            this.status = AssemblyProcessStatus.COMPLETED;
        }

        // Check overtime: BR-09
        boolean overtimeAlert = targetStep.isOvertime();

        return new AssemblyStepResult(overtimeAlert, stationCompleted, allCompleted);
    }

    private AssemblyStep findStep(AssemblyStepId stepId) {
        return steps.stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Assembly step not found: " + stepId.value()));
    }

    /**
     * BR-07: All steps at stations with lower sequence must be completed before working on this station.
     */
    private void validateStationSequence(int targetStationSequence) {
        boolean previousStationsCompleted = steps.stream()
            .filter(s -> s.getWorkStation().sequence() < targetStationSequence)
            .allMatch(AssemblyStep::isCompleted);

        if (!previousStationsCompleted) {
            throw new IllegalStateException(
                "Cannot complete step at station " + targetStationSequence +
                ": previous station steps are not all completed (BR-07)");
        }
    }

    private boolean areAllStepsAtStationCompleted(int stationSequence) {
        return steps.stream()
            .filter(s -> s.getWorkStation().sequence() == stationSequence)
            .allMatch(AssemblyStep::isCompleted);
    }

    private boolean areAllStepsCompleted() {
        return steps.stream().allMatch(AssemblyStep::isCompleted);
    }

    public AssemblyProcessId getId() {
        return id;
    }

    public AssemblyProcessStatus getStatus() {
        return status;
    }

    public List<AssemblyStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Finds the step matching the given step ID.
     */
    public AssemblyStep getStep(AssemblyStepId stepId) {
        return findStep(stepId);
    }
}
