package com.automfg.manufacturing.application.usecase;

import java.util.UUID;

public interface CompleteReworkUseCase {
    record CompleteReworkCommand(UUID reworkOrderId) {}
    void execute(CompleteReworkCommand command);
}
