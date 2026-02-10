package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class AssemblyOvertimeAlertEvent extends DomainEvent {

    private final UUID productionOrderId;
    private final String stepDescription;
    private final int standardMinutes;
    private final int actualMinutes;

    public AssemblyOvertimeAlertEvent(UUID productionOrderId, String stepDescription,
                                      int standardMinutes, int actualMinutes) {
        super();
        this.productionOrderId = productionOrderId;
        this.stepDescription = stepDescription;
        this.standardMinutes = standardMinutes;
        this.actualMinutes = actualMinutes;
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public int getStandardMinutes() {
        return standardMinutes;
    }

    public int getActualMinutes() {
        return actualMinutes;
    }
}
