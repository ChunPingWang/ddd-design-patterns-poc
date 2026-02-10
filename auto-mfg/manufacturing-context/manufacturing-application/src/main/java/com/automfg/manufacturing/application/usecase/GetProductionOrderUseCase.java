package com.automfg.manufacturing.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GetProductionOrderUseCase extends QueryUseCase {

    record GetProductionOrderQuery(UUID productionOrderId) {}

    record ProductionOrderDetail(
        UUID id, String orderNumber, UUID sourceOrderId, String vin,
        String status, Integer currentStationSequence,
        LocalDateTime scheduledStartDate, LocalDateTime createdAt,
        String assemblyProcessStatus, int totalAssemblySteps
    ) {}

    ProductionOrderDetail execute(GetProductionOrderQuery query);
}
