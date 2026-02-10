package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface ReviewInspectionUseCase {
    record ReviewInspectionCommand(UUID inspectionId, String reviewerId) {}
    record ReviewInspectionResult(UUID inspectionId, String result, String reviewerId) {}
    ReviewInspectionResult execute(ReviewInspectionCommand command);
}
