package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class InspectionCreatedEvent extends DomainEvent {

    private final UUID inspectionId;
    private final UUID productionOrderId;
    private final String vin;
    private final String inspectorId;

    public InspectionCreatedEvent(UUID inspectionId, UUID productionOrderId,
                                   String vin, String inspectorId) {
        super();
        this.inspectionId = inspectionId;
        this.productionOrderId = productionOrderId;
        this.vin = vin;
        this.inspectorId = inspectorId;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getVin() {
        return vin;
    }

    public String getInspectorId() {
        return inspectorId;
    }
}
