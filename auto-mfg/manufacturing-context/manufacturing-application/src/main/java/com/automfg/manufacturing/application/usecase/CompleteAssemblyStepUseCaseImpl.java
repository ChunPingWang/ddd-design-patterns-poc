package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.AssemblyStepId;
import com.automfg.manufacturing.domain.model.AssemblyStepResult;
import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.shared.domain.DomainEventPublisher;

public class CompleteAssemblyStepUseCaseImpl implements CompleteAssemblyStepUseCase {

    private final ProductionOrderRepository productionOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public CompleteAssemblyStepUseCaseImpl(ProductionOrderRepository productionOrderRepository,
                                           DomainEventPublisher domainEventPublisher) {
        this.productionOrderRepository = productionOrderRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public CompleteAssemblyStepResult execute(CompleteAssemblyStepCommand command) {
        ProductionOrder order = productionOrderRepository
            .findById(new ProductionOrderId(command.productionOrderId()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Production order not found: " + command.productionOrderId()));

        AssemblyStepResult result = order.completeAssemblyStep(
            new AssemblyStepId(command.assemblyStepId()),
            command.operatorId(),
            command.materialBatchId(),
            command.actualMinutes()
        );

        productionOrderRepository.save(order);
        domainEventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return new CompleteAssemblyStepResult(
            order.getId().value(),
            command.assemblyStepId(),
            order.getStatus().name(),
            result.overtimeAlert(),
            result.stationCompleted(),
            result.assemblyCompleted()
        );
    }
}
