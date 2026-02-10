package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public class MaterialShortageEvent extends DomainEvent {

    private final UUID productionOrderId;
    private final UUID sourceOrderId;
    private final List<String> missingParts;

    public MaterialShortageEvent(UUID productionOrderId, UUID sourceOrderId,
                                 List<String> missingParts) {
        super();
        this.productionOrderId = productionOrderId;
        this.sourceOrderId = sourceOrderId;
        this.missingParts = List.copyOf(missingParts);
    }

    public UUID getProductionOrderId() {
        return productionOrderId;
    }

    public UUID getSourceOrderId() {
        return sourceOrderId;
    }

    public List<String> getMissingParts() {
        return missingParts;
    }
}
