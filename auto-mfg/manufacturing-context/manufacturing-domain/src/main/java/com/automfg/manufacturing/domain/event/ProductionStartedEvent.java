package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class ProductionStartedEvent extends DomainEvent {

    private final UUID productionOrderId;
    private final String vin;
    private final String operatorId;

    public ProductionStartedEvent(UUID productionOrderId, String vin, String operatorId) {
        super();
        this.productionOrderId = productionOrderId;
        this.vin = vin;
        this.operatorId = operatorId;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getVin() {
        return vin;
    }

    public String getOperatorId() {
        return operatorId;
    }
}
