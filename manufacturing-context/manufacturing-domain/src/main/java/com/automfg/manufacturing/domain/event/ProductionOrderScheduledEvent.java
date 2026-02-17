package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class ProductionOrderScheduledEvent extends DomainEvent {

    private final UUID productionOrderId;
    private final String orderNumber;
    private final UUID sourceOrderId;
    private final String vin;

    public ProductionOrderScheduledEvent(UUID productionOrderId, String orderNumber,
                                         UUID sourceOrderId, String vin) {
        super();
        this.productionOrderId = productionOrderId;
        this.orderNumber = orderNumber;
        this.sourceOrderId = sourceOrderId;
        this.vin = vin;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getSourceOrderId() {
        return sourceOrderId;
    }

    public String getVin() {
        return vin;
    }
}
