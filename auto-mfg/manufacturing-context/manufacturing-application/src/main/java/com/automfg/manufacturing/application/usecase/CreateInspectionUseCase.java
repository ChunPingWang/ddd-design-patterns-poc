package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface CreateInspectionUseCase {
    record CreateInspectionCommand(UUID productionOrderId, String vehicleModelCode, String inspectorId) {}
    record CreateInspectionResult(UUID inspectionId, int itemCount) {}
    CreateInspectionResult execute(CreateInspectionCommand command);
}
