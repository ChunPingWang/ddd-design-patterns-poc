package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.*;
import com.automfg.manufacturing.domain.port.InspectionChecklistGateway;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import com.automfg.shared.domain.DomainEventPublisher;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CreateInspectionUseCaseImpl implements CreateInspectionUseCase {

    private final ProductionOrderRepository productionOrderRepository;
    private final QualityInspectionRepository qualityInspectionRepository;
    private final InspectionChecklistGateway inspectionChecklistGateway;
    private final DomainEventPublisher domainEventPublisher;

    public CreateInspectionUseCaseImpl(ProductionOrderRepository productionOrderRepository,
                                       QualityInspectionRepository qualityInspectionRepository,
                                       InspectionChecklistGateway inspectionChecklistGateway,
                                       DomainEventPublisher domainEventPublisher) {
        this.productionOrderRepository = Objects.requireNonNull(productionOrderRepository);
        this.qualityInspectionRepository = Objects.requireNonNull(qualityInspectionRepository);
        this.inspectionChecklistGateway = Objects.requireNonNull(inspectionChecklistGateway);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
    }

    @Override
    public CreateInspectionResult execute(CreateInspectionCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        // Load production order
        ProductionOrderId productionOrderId = new ProductionOrderId(command.productionOrderId());
        ProductionOrder productionOrder = productionOrderRepository.findById(productionOrderId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Production order not found: " + command.productionOrderId()));

        // Validate status
        if (productionOrder.getStatus() != ProductionOrderStatus.ASSEMBLY_COMPLETED) {
            throw new IllegalStateException(
                "Production order must be in ASSEMBLY_COMPLETED status to create inspection. Current: "
                    + productionOrder.getStatus());
        }

        // Load checklist from vehicle configuration via ACL
        String vehicleModelCode = command.vehicleModelCode();
        List<ChecklistItemTemplate> checklistItems =
            inspectionChecklistGateway.getChecklistForModel(vehicleModelCode);
        if (checklistItems.isEmpty()) {
            throw new IllegalStateException(
                "No checklist items found for model: " + vehicleModelCode);
        }

        // Create inspection
        QualityInspectionId inspectionId = new QualityInspectionId(UUID.randomUUID());
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, productionOrder.getVin(),
            command.inspectorId(), checklistItems);

        // Persist and publish events
        qualityInspectionRepository.save(inspection);
        domainEventPublisher.publishAll(inspection.getDomainEvents());
        inspection.clearDomainEvents();

        return new CreateInspectionResult(inspectionId.value(), inspection.getItems().size());
    }
}
