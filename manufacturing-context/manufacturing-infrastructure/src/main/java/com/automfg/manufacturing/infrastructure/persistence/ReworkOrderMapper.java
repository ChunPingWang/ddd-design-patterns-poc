package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between ReworkOrder domain model and JPA entity.
 * Failed items are stored as comma-separated TEXT in the database.
 */
public class ReworkOrderMapper {

    private static final String DELIMITER = ",";

    private ReworkOrderMapper() {
        // Utility class
    }

    public static ReworkOrderJpaEntity toJpaEntity(ReworkOrder domain) {
        ReworkOrderJpaEntity entity = new ReworkOrderJpaEntity();
        entity.setId(domain.getId());
        entity.setProductionOrderId(domain.getProductionOrderId().value());
        entity.setInspectionId(domain.getInspectionId().value());
        entity.setStatus(domain.getStatus().name());
        entity.setFailedItems(String.join(DELIMITER, domain.getFailedItemDescriptions()));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        return entity;
    }

    public static ReworkOrder toDomain(ReworkOrderJpaEntity entity) {
        List<String> failedItems;
        if (entity.getFailedItems() != null && !entity.getFailedItems().isBlank()) {
            failedItems = Arrays.asList(entity.getFailedItems().split(DELIMITER));
        } else {
            failedItems = Collections.emptyList();
        }

        return ReworkOrder.reconstitute(
            entity.getId(),
            new ProductionOrderId(entity.getProductionOrderId()),
            new QualityInspectionId(entity.getInspectionId()),
            ReworkStatus.valueOf(entity.getStatus()),
            failedItems,
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }
}
