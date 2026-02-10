package com.automfg.manufacturing.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssemblyProcessTest {

    private AssemblyProcess process;
    private List<AssemblyStepTemplate> templates;

    @BeforeEach
    void setUp() {
        templates = List.of(
            new AssemblyStepTemplate("WS-BODY", 1, "Body welding", 60),
            new AssemblyStepTemplate("WS-BODY", 1, "Body frame alignment", 30),
            new AssemblyStepTemplate("WS-PAINT", 2, "Paint application", 45),
            new AssemblyStepTemplate("WS-TRIM", 3, "Interior trim installation", 30)
        );
        process = new AssemblyProcess(new AssemblyProcessId(UUID.randomUUID()), templates);
    }

    @Test
    @DisplayName("start transitions NOT_STARTED to IN_PROGRESS")
    void start_process() {
        assertThat(process.getStatus()).isEqualTo(AssemblyProcessStatus.NOT_STARTED);

        process.start();

        assertThat(process.getStatus()).isEqualTo(AssemblyProcessStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("completeStep successfully completes a step and records batch")
    void complete_step_success() {
        process.start();
        AssemblyStepId stepId = process.getSteps().get(0).getId();

        AssemblyStepResult result = process.completeStep(stepId, "OP-001", "BATCH-2026-001", 55);

        AssemblyStep completedStep = process.getStep(stepId);
        assertThat(completedStep.getStatus()).isEqualTo(AssemblyStepStatus.COMPLETED);
        assertThat(completedStep.getOperatorId()).isEqualTo("OP-001");
        assertThat(completedStep.getMaterialBatchId().value()).isEqualTo("BATCH-2026-001");
        assertThat(completedStep.getActualTimeMinutes()).isEqualTo(55);
        assertThat(result.overtimeAlert()).isFalse();
    }

    @Test
    @DisplayName("completeStep throws when material batch ID is missing (BR-08)")
    void complete_step_missing_batch_throws() {
        process.start();
        AssemblyStepId stepId = process.getSteps().get(0).getId();

        assertThatThrownBy(() -> process.completeStep(stepId, "OP-001", "", 55))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Material batch ID is required");

        assertThatThrownBy(() -> process.completeStep(stepId, "OP-001", null, 55))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Material batch ID is required");
    }

    @Test
    @DisplayName("station sequence is enforced - cannot skip stations (BR-07)")
    void station_sequence_enforcement() {
        process.start();
        // Try to complete a step at station 2 before station 1 is done
        AssemblyStepId station2StepId = process.getSteps().get(2).getId(); // station 2

        assertThatThrownBy(() -> process.completeStep(station2StepId, "OP-001", "BATCH-001", 40))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("previous station steps are not all completed")
            .hasMessageContaining("BR-07");
    }

    @Test
    @DisplayName("all steps completed transitions process to COMPLETED")
    void all_steps_completed() {
        process.start();
        List<AssemblyStep> steps = process.getSteps();

        // Complete station 1 steps (indices 0, 1)
        process.completeStep(steps.get(0).getId(), "OP-001", "BATCH-001", 55);
        process.completeStep(steps.get(1).getId(), "OP-001", "BATCH-002", 25);

        // Complete station 2 step (index 2)
        process.completeStep(steps.get(2).getId(), "OP-002", "BATCH-003", 40);

        // Complete station 3 step (index 3) — this should be the last one
        AssemblyStepResult result = process.completeStep(steps.get(3).getId(), "OP-003", "BATCH-004", 28);

        assertThat(result.assemblyCompleted()).isTrue();
        assertThat(process.getStatus()).isEqualTo(AssemblyProcessStatus.COMPLETED);
    }
}
