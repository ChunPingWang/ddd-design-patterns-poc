package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface StartProductionUseCase {

    record StartProductionCommand(UUID productionOrderId, String operatorId, String workstationCode) {}

    record StartProductionResult(UUID productionOrderId, String status) {}

    StartProductionResult execute(StartProductionCommand command);
}
