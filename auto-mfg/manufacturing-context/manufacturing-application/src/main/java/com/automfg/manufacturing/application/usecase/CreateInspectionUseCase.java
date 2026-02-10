package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface CreateInspectionUseCase extends CommandUseCase {
    record CreateInspectionCommand(UUID productionOrderId, String vehicleModelCode, String inspectorId) {}
    record CreateInspectionResult(UUID inspectionId, int itemCount) {}
    CreateInspectionResult execute(CreateInspectionCommand command);
}
