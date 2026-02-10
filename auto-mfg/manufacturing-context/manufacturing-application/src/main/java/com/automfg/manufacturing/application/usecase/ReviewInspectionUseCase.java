package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface ReviewInspectionUseCase extends CommandUseCase {
    record ReviewInspectionCommand(UUID inspectionId, String reviewerId) {}
    record ReviewInspectionResult(UUID inspectionId, String result, String reviewerId) {}
    ReviewInspectionResult execute(ReviewInspectionCommand command);
}
