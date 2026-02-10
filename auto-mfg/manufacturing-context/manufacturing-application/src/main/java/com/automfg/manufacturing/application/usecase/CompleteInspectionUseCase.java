package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface CompleteInspectionUseCase {
    record CompleteInspectionCommand(UUID inspectionId, String inspectorId) {}
    record CompleteInspectionResult(UUID inspectionId, String result) {}
    CompleteInspectionResult execute(CompleteInspectionCommand command);
}
