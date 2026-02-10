package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface RecordInspectionItemResultUseCase {
    record RecordItemResultCommand(UUID inspectionId, UUID itemId, String status, String notes) {}
    void execute(RecordItemResultCommand command);
}
