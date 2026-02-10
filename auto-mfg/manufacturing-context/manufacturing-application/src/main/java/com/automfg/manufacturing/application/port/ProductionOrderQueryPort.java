package com.automfg.manufacturing.application.port;

import com.automfg.manufacturing.application.usecase.GetAssemblyStepsUseCase;
import com.automfg.manufacturing.application.usecase.GetProductionOrderUseCase;
import com.automfg.manufacturing.application.usecase.ListProductionOrdersUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-side port for production order reads (CQRS read path).
 * Unlike the domain's ProductionOrderRepository (which returns domain objects),
 * this port returns read models (DTOs) directly — a key CQRS optimization.
 */
public interface ProductionOrderQueryPort {

    Optional<GetProductionOrderUseCase.ProductionOrderDetail> findById(UUID id);

    List<ListProductionOrdersUseCase.ProductionOrderSummary> findByStatus(String status);

    List<ListProductionOrdersUseCase.ProductionOrderSummary> findAll();

    List<GetAssemblyStepsUseCase.AssemblyStepDetail> findAssemblySteps(UUID productionOrderId, String stationCode);
}
