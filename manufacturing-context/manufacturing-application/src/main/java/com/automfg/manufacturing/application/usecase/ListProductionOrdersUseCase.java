package com.automfg.manufacturing.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ListProductionOrdersUseCase extends QueryUseCase {

    record ListProductionOrdersQuery(String status) {}

    record ProductionOrderSummary(
        UUID id, String orderNumber, String vin,
        String status, Integer currentStationSequence,
        LocalDateTime createdAt
    ) {}

    List<ProductionOrderSummary> execute(ListProductionOrdersQuery query);
}
