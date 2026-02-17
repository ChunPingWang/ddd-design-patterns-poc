package com.automfg.manufacturing.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.util.UUID;

public interface GetInspectionUseCase extends QueryUseCase {

    record GetInspectionQuery(UUID inspectionId) {}

    record InspectionDetail(
        UUID id, UUID productionOrderId, String vin,
        String result, String inspectorId, String reviewerId,
        int itemCount
    ) {}

    InspectionDetail execute(GetInspectionQuery query);
}
