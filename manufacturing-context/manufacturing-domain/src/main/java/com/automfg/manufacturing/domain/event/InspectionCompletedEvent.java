package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class InspectionCompletedEvent extends DomainEvent {

    private final UUID inspectionId;
    private final UUID productionOrderId;
    private final String result;

    public InspectionCompletedEvent(UUID inspectionId, UUID productionOrderId, String result) {
        super();
        this.inspectionId = inspectionId;
        this.productionOrderId = productionOrderId;
        this.result = result;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getResult() {
        return result;
    }
}
