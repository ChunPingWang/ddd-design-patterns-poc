package com.automfg.manufacturing.domain.service;

import com.automfg.manufacturing.domain.model.InspectionResult;
import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;

import java.util.Objects;

/**
 * Cross-aggregate domain service that coordinates the completion of an inspection review
 * and the corresponding production order status transition.
 *
 * Pure Java — NO framework dependencies.
 */
public class InspectionCompletionService {

    private final ProductionOrderRepository productionOrderRepository;

    public InspectionCompletionService(ProductionOrderRepository productionOrderRepository) {
        this.productionOrderRepository = Objects.requireNonNull(productionOrderRepository,
            "ProductionOrderRepository must not be null");
    }

    /**
     * Completes the inspection review by updating the production order status based on
     * the inspection result.
     *
     * @param inspection the completed and reviewed quality inspection
     * @param productionOrder the production order associated with the inspection
     */
    public void completeInspectionReview(QualityInspection inspection, ProductionOrder productionOrder) {
        Objects.requireNonNull(inspection, "QualityInspection must not be null");
        Objects.requireNonNull(productionOrder, "ProductionOrder must not be null");

        InspectionResult result = inspection.getResult();
        if (result == null) {
            throw new IllegalStateException("Inspection has not been completed yet");
        }

        if (result == InspectionResult.PASSED || result == InspectionResult.CONDITIONAL_PASS) {
            productionOrder.markInspectionPassed();
        } else if (result == InspectionResult.FAILED) {
            productionOrder.markInspectionFailed();
        }

        productionOrderRepository.save(productionOrder);
    }
}
