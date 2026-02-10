package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.model.QualityInspectionId;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import com.automfg.shared.domain.DomainEventPublisher;

import java.util.Objects;

public class CompleteInspectionUseCaseImpl implements CompleteInspectionUseCase {

    private final QualityInspectionRepository qualityInspectionRepository;
    private final DomainEventPublisher domainEventPublisher;

    public CompleteInspectionUseCaseImpl(QualityInspectionRepository qualityInspectionRepository,
                                         DomainEventPublisher domainEventPublisher) {
        this.qualityInspectionRepository = Objects.requireNonNull(qualityInspectionRepository);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
    }

    @Override
    public CompleteInspectionResult execute(CompleteInspectionCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        QualityInspectionId inspectionId = new QualityInspectionId(command.inspectionId());
        QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection not found: " + command.inspectionId()));

        inspection.complete(command.inspectorId());

        qualityInspectionRepository.save(inspection);
        domainEventPublisher.publishAll(inspection.getDomainEvents());
        inspection.clearDomainEvents();

        return new CompleteInspectionResult(
            inspectionId.value(), inspection.getResult().name());
    }
}
