package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.*;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import com.automfg.manufacturing.domain.port.ReworkOrderRepository;
import com.automfg.shared.domain.DomainEventPublisher;

import java.util.Objects;
import java.util.UUID;

public class ReviewInspectionUseCaseImpl implements ReviewInspectionUseCase {

    private final QualityInspectionRepository qualityInspectionRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ReworkOrderRepository reworkOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public ReviewInspectionUseCaseImpl(QualityInspectionRepository qualityInspectionRepository,
                                       ProductionOrderRepository productionOrderRepository,
                                       ReworkOrderRepository reworkOrderRepository,
                                       DomainEventPublisher domainEventPublisher) {
        this.qualityInspectionRepository = Objects.requireNonNull(qualityInspectionRepository);
        this.productionOrderRepository = Objects.requireNonNull(productionOrderRepository);
        this.reworkOrderRepository = Objects.requireNonNull(reworkOrderRepository);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
    }

    @Override
    public ReviewInspectionResult execute(ReviewInspectionCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        QualityInspectionId inspectionId = new QualityInspectionId(command.inspectionId());
        QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection not found: " + command.inspectionId()));

        // Perform review on the inspection aggregate
        inspection.review(command.reviewerId());

        // Load production order and update its status
        ProductionOrder productionOrder = productionOrderRepository.findById(inspection.getProductionOrderId())
            .orElseThrow(() -> new IllegalStateException(
                "Production order not found: " + inspection.getProductionOrderId().value()));

        if (inspection.getResult() == InspectionResult.PASSED
                || inspection.getResult() == InspectionResult.CONDITIONAL_PASS) {
            productionOrder.markInspectionPassed();
        } else if (inspection.getResult() == InspectionResult.FAILED) {
            productionOrder.markInspectionFailed();

            // Create rework order for failed inspections
            ReworkOrder reworkOrder = ReworkOrder.create(
                UUID.randomUUID(),
                inspection.getProductionOrderId(),
                inspectionId,
                inspection.getFailedItemDescriptions());

            reworkOrderRepository.save(reworkOrder);
            domainEventPublisher.publishAll(reworkOrder.getDomainEvents());
            reworkOrder.clearDomainEvents();
        }

        // Persist changes
        qualityInspectionRepository.save(inspection);
        productionOrderRepository.save(productionOrder);

        // Publish inspection events
        domainEventPublisher.publishAll(inspection.getDomainEvents());
        inspection.clearDomainEvents();

        // Publish production order events (if any)
        domainEventPublisher.publishAll(productionOrder.getDomainEvents());
        productionOrder.clearDomainEvents();

        return new ReviewInspectionResult(
            inspectionId.value(), inspection.getResult().name(), command.reviewerId());
    }
}
