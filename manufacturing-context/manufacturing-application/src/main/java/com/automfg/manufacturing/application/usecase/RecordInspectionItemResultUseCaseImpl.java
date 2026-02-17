package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.InspectionItemId;
import com.automfg.manufacturing.domain.model.InspectionItemStatus;
import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.model.QualityInspectionId;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import com.automfg.shared.domain.DomainEventPublisher;

import java.util.Objects;

public class RecordInspectionItemResultUseCaseImpl implements RecordInspectionItemResultUseCase {

    private final QualityInspectionRepository qualityInspectionRepository;
    private final DomainEventPublisher domainEventPublisher;

    public RecordInspectionItemResultUseCaseImpl(QualityInspectionRepository qualityInspectionRepository,
                                                  DomainEventPublisher domainEventPublisher) {
        this.qualityInspectionRepository = Objects.requireNonNull(qualityInspectionRepository);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
    }

    @Override
    public void execute(RecordItemResultCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        QualityInspectionId inspectionId = new QualityInspectionId(command.inspectionId());
        QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection not found: " + command.inspectionId()));

        InspectionItemId itemId = new InspectionItemId(command.itemId());
        InspectionItemStatus status = InspectionItemStatus.valueOf(command.status());

        inspection.recordItemResult(itemId, status, command.notes());

        qualityInspectionRepository.save(inspection);
        domainEventPublisher.publishAll(inspection.getDomainEvents());
        inspection.clearDomainEvents();
    }
}
