package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface RecordInspectionItemResultUseCase extends CommandUseCase {
    record RecordItemResultCommand(UUID inspectionId, UUID itemId, String status, String notes) {}
    void execute(RecordItemResultCommand command);
}
