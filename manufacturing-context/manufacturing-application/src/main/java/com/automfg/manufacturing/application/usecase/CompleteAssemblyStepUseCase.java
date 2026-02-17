package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface CompleteAssemblyStepUseCase extends CommandUseCase {

    record CompleteAssemblyStepCommand(
        UUID productionOrderId,
        UUID assemblyStepId,
        String operatorId,
        String materialBatchId,
        int actualMinutes
    ) {}

    record CompleteAssemblyStepResult(
        UUID productionOrderId,
        UUID assemblyStepId,
        String orderStatus,
        boolean overtimeAlert,
        boolean stationCompleted,
        boolean assemblyCompleted
    ) {}

    CompleteAssemblyStepResult execute(CompleteAssemblyStepCommand command);
}
