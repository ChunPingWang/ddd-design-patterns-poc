package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.application.port.InspectionQueryPort;
import com.automfg.manufacturing.application.usecase.GetInspectionUseCase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Query-side adapter for inspections: reads JPA entities and maps to DTOs.
 */
@Repository
public class JpaInspectionQueryAdapter implements InspectionQueryPort {

    private final QualityInspectionJpaRepository jpaRepository;

    public JpaInspectionQueryAdapter(QualityInspectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<GetInspectionUseCase.InspectionDetail> findById(UUID id) {
        return jpaRepository.findById(id).map(e -> new GetInspectionUseCase.InspectionDetail(
            e.getId(), e.getProductionOrderId(), e.getVin(),
            e.getResult(), e.getInspectorId(), e.getReviewerId(),
            e.getItems().size()
        ));
    }
}
