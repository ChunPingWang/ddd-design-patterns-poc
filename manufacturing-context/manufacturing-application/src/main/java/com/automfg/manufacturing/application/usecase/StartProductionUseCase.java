package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface StartProductionUseCase extends CommandUseCase {

    record StartProductionCommand(UUID productionOrderId, String operatorId, String workstationCode) {}

    record StartProductionResult(UUID productionOrderId, String status) {}

    StartProductionResult execute(StartProductionCommand command);
}
