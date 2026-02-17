package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public class InspectionFailedEvent extends DomainEvent {

    private final UUID inspectionId;
    private final UUID productionOrderId;
    private final String vin;
    private final List<String> failedItemDescriptions;

    public InspectionFailedEvent(UUID inspectionId, UUID productionOrderId,
                                  String vin, List<String> failedItemDescriptions) {
        super();
        this.inspectionId = inspectionId;
        this.productionOrderId = productionOrderId;
        this.vin = vin;
        this.failedItemDescriptions = List.copyOf(failedItemDescriptions);
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

    public List<String> getFailedItemDescriptions() {
        return failedItemDescriptions;
    }
}
