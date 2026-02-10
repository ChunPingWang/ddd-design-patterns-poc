package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface CompleteReworkUseCase extends CommandUseCase {
    record CompleteReworkCommand(UUID reworkOrderId) {}
    void execute(CompleteReworkCommand command);
}
