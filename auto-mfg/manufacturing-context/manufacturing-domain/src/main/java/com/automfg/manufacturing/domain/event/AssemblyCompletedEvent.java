package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class AssemblyCompletedEvent extends DomainEvent {

    private final UUID productionOrderId;
    private final String vin;

    public AssemblyCompletedEvent(UUID productionOrderId, String vin) {
        super();
        this.productionOrderId = productionOrderId;
        this.vin = vin;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getVin() {
        return vin;
    }
}
