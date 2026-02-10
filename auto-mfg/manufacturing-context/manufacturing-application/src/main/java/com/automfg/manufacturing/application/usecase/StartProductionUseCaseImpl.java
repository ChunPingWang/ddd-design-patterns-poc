package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.shared.domain.DomainEventPublisher;

public class StartProductionUseCaseImpl implements StartProductionUseCase {

    private final ProductionOrderRepository productionOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public StartProductionUseCaseImpl(ProductionOrderRepository productionOrderRepository,
                                      DomainEventPublisher domainEventPublisher) {
        this.productionOrderRepository = productionOrderRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public StartProductionResult execute(StartProductionCommand command) {
        ProductionOrder order = productionOrderRepository
            .findById(new ProductionOrderId(command.productionOrderId()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Production order not found: " + command.productionOrderId()));

        order.startProduction(command.operatorId(), command.workstationCode());

        productionOrderRepository.save(order);
        domainEventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return new StartProductionResult(
            order.getId().value(),
            order.getStatus().name()
        );
    }
}
