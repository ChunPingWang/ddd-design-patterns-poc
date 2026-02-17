package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class ReworkOrderCreatedEvent extends DomainEvent {

    private final UUID reworkOrderId;
    private final UUID productionOrderId;
    private final UUID inspectionId;

    public ReworkOrderCreatedEvent(UUID reworkOrderId, UUID productionOrderId, UUID inspectionId) {
        super();
        this.reworkOrderId = reworkOrderId;
        this.productionOrderId = productionOrderId;
        this.inspectionId = inspectionId;
    }

    public UUID getReworkOrderId() {
        return reworkOrderId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public UUID getInspectionId() {
        return inspectionId;
    }
}
