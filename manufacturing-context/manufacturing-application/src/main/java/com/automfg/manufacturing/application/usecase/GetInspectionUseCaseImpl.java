package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.application.port.InspectionQueryPort;

public class GetInspectionUseCaseImpl implements GetInspectionUseCase {

    private final InspectionQueryPort queryPort;

    public GetInspectionUseCaseImpl(InspectionQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public InspectionDetail execute(GetInspectionQuery query) {
        return queryPort.findById(query.inspectionId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection not found: " + query.inspectionId()));
    }
}
