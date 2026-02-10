package com.automfg.manufacturing.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.UUID;

public class InspectionReviewedEvent extends DomainEvent {

    private final UUID inspectionId;
    private final UUID productionOrderId;
    private final String result;
    private final String reviewerId;

    public InspectionReviewedEvent(UUID inspectionId, UUID productionOrderId,
                                    String result, String reviewerId) {
        super();
        this.inspectionId = inspectionId;
        this.productionOrderId = productionOrderId;
        this.result = result;
        this.reviewerId = reviewerId;
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

    public String getReviewerId() {
        return reviewerId;
    }
}
