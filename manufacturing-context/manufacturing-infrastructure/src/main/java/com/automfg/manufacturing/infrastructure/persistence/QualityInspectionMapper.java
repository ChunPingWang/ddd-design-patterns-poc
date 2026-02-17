package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between QualityInspection domain model and JPA entities.
 * Handles bidirectional mapping for the aggregate root and its child entities.
 */
public class QualityInspectionMapper {

    private QualityInspectionMapper() {
        // Utility class
    }

    public static QualityInspectionJpaEntity toJpaEntity(QualityInspection domain) {
        QualityInspectionJpaEntity entity = new QualityInspectionJpaEntity();
        entity.setId(domain.getId().value());
        entity.setProductionOrderId(domain.getProductionOrderId().value());
        entity.setVin(domain.getVin().value());
        entity.setResult(domain.getResult() != null ? domain.getResult().name() : null);
        entity.setInspectorId(domain.getInspectorId());
        entity.setReviewerId(domain.getReviewerId());
        entity.setInspectedAt(domain.getInspectedAt());
        entity.setReviewedAt(domain.getReviewedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCorrectsRecordId(domain.getCorrectsRecordId());

        List<InspectionItemJpaEntity> itemEntities = domain.getItems().stream()
            .map(item -> toItemJpaEntity(item, entity))
            .collect(Collectors.toList());
        entity.setItems(itemEntities);

        return entity;
    }

    private static InspectionItemJpaEntity toItemJpaEntity(InspectionItem item,
                                                            QualityInspectionJpaEntity parentEntity) {
        InspectionItemJpaEntity entity = new InspectionItemJpaEntity();
        entity.setId(item.getId().value());
        entity.setInspection(parentEntity);
        entity.setDescription(item.getDescription());
        entity.setSafetyRelated(item.isSafetyRelated());
        entity.setStatus(item.getStatus().name());
        entity.setNotes(item.getNotes());
        return entity;
    }

    public static QualityInspection toDomain(QualityInspectionJpaEntity entity) {
        List<InspectionItem> items = entity.getItems().stream()
            .map(QualityInspectionMapper::toItemDomain)
            .toList();

        return QualityInspection.reconstitute(
            new QualityInspectionId(entity.getId()),
            new ProductionOrderId(entity.getProductionOrderId()),
            new VIN(entity.getVin()),
            entity.getResult() != null ? InspectionResult.valueOf(entity.getResult()) : null,
            entity.getInspectorId(),
            entity.getReviewerId(),
            entity.getInspectedAt(),
            entity.getReviewedAt(),
            entity.getCreatedAt(),
            items,
            entity.getCorrectsRecordId()
        );
    }

    private static InspectionItem toItemDomain(InspectionItemJpaEntity entity) {
        return InspectionItem.reconstitute(
            new InspectionItemId(entity.getId()),
            entity.getDescription(),
            entity.isSafetyRelated(),
            InspectionItemStatus.valueOf(entity.getStatus()),
            entity.getNotes()
        );
    }
}
