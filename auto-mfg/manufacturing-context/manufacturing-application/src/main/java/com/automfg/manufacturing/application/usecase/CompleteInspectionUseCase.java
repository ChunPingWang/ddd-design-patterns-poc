package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface CompleteInspectionUseCase extends CommandUseCase {
    record CompleteInspectionCommand(UUID inspectionId, String inspectorId) {}
    record CompleteInspectionResult(UUID inspectionId, String result) {}
    CompleteInspectionResult execute(CompleteInspectionCommand command);
}
