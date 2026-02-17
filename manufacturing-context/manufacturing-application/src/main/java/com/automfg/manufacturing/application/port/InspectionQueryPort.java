package com.automfg.manufacturing.application.port;

import com.automfg.manufacturing.application.usecase.GetInspectionUseCase;

import java.util.Optional;
import java.util.UUID;

/**
 * Query-side port for inspection reads (CQRS read path).
 */
public interface InspectionQueryPort {
    Optional<GetInspectionUseCase.InspectionDetail> findById(UUID id);
}
