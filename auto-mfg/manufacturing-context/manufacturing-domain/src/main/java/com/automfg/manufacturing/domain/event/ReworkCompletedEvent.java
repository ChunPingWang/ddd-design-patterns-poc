package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class ReworkCompletedEvent extends DomainEvent {

    private final UUID reworkOrderId;
    private final UUID productionOrderId;

    public ReworkCompletedEvent(UUID reworkOrderId, UUID productionOrderId) {
        super();
        this.reworkOrderId = reworkOrderId;
        this.productionOrderId = productionOrderId;
    }

    public UUID getReworkOrderId() {
        return reworkOrderId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }
}
