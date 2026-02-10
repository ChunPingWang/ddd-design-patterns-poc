package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.application.port.ProductionOrderQueryPort;
import com.automfg.manufacturing.application.usecase.GetAssemblyStepsUseCase;
import com.automfg.manufacturing.application.usecase.GetProductionOrderUseCase;
import com.automfg.manufacturing.application.usecase.ListProductionOrdersUseCase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-side adapter: reads JPA entities and maps directly to DTOs.
 * Does NOT reconstitute domain objects — this is a key CQRS optimization.
 */
@Repository
public class JpaProductionOrderQueryAdapter implements ProductionOrderQueryPort {

    private final ProductionOrderJpaRepository jpaRepository;

    public JpaProductionOrderQueryAdapter(ProductionOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<GetProductionOrderUseCase.ProductionOrderDetail> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDetail);
    }

    @Override
    public List<ListProductionOrdersUseCase.ProductionOrderSummary> findByStatus(String status) {
        return jpaRepository.findByStatus(status).stream()
            .map(this::toSummary).toList();
    }

    @Override
    public List<ListProductionOrdersUseCase.ProductionOrderSummary> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toSummary).toList();
    }

    @Override
    public List<GetAssemblyStepsUseCase.AssemblyStepDetail> findAssemblySteps(
            UUID productionOrderId, String stationCode) {
        return jpaRepository.findById(productionOrderId)
            .map(e -> {
                AssemblyProcessJpaEntity ap = e.getAssemblyProcess();
                if (ap == null) return List.<GetAssemblyStepsUseCase.AssemblyStepDetail>of();

                List<AssemblyStepJpaEntity> steps = ap.getSteps();
                if (stationCode != null && !stationCode.isBlank()) {
                    steps = steps.stream()
                        .filter(s -> s.getWorkStationCode().equals(stationCode))
                        .toList();
                }
                return steps.stream().map(this::toStepDetail).toList();
            })
            .orElse(List.of());
    }

    private GetProductionOrderUseCase.ProductionOrderDetail toDetail(ProductionOrderJpaEntity e) {
        AssemblyProcessJpaEntity ap = e.getAssemblyProcess();
        return new GetProductionOrderUseCase.ProductionOrderDetail(
            e.getId(), e.getOrderNumber(), e.getSourceOrderId(), e.getVin(),
            e.getStatus(), e.getCurrentStationSequence(),
            e.getScheduledStartDate(), e.getCreatedAt(),
            ap != null ? ap.getStatus() : null,
            ap != null ? ap.getSteps().size() : 0
        );
    }

    private ListProductionOrdersUseCase.ProductionOrderSummary toSummary(ProductionOrderJpaEntity e) {
        return new ListProductionOrdersUseCase.ProductionOrderSummary(
            e.getId(), e.getOrderNumber(), e.getVin(),
            e.getStatus(), e.getCurrentStationSequence(), e.getCreatedAt()
        );
    }

    private GetAssemblyStepsUseCase.AssemblyStepDetail toStepDetail(AssemblyStepJpaEntity s) {
        return new GetAssemblyStepsUseCase.AssemblyStepDetail(
            s.getId(), s.getWorkStationCode(), s.getWorkStationSequence(),
            s.getTaskDescription(), s.getStandardTimeMinutes(),
            s.getStatus(), s.getOperatorId(), s.getMaterialBatchId(),
            s.getActualTimeMinutes(), s.getCompletedAt()
        );
    }
}
