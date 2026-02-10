package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ReworkOrder;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.manufacturing.domain.port.ReworkOrderRepository;
import com.automfg.shared.domain.DomainEventPublisher;

import java.util.Objects;

public class CompleteReworkUseCaseImpl implements CompleteReworkUseCase {

    private final ReworkOrderRepository reworkOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public CompleteReworkUseCaseImpl(ReworkOrderRepository reworkOrderRepository,
                                     ProductionOrderRepository productionOrderRepository,
                                     DomainEventPublisher domainEventPublisher) {
        this.reworkOrderRepository = Objects.requireNonNull(reworkOrderRepository);
        this.productionOrderRepository = Objects.requireNonNull(productionOrderRepository);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
    }

    @Override
    public void execute(CompleteReworkCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        ReworkOrder reworkOrder = reworkOrderRepository.findById(command.reworkOrderId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Rework order not found: " + command.reworkOrderId()));

        // Complete the rework order
        reworkOrder.complete();

        // Transition production order back to ASSEMBLY_COMPLETED via completeRework
        ProductionOrder productionOrder = productionOrderRepository.findById(reworkOrder.getProductionOrderId())
            .orElseThrow(() -> new IllegalStateException(
                "Production order not found: " + reworkOrder.getProductionOrderId().value()));

        productionOrder.startRework();
        productionOrder.completeRework();

        // Persist
        reworkOrderRepository.save(reworkOrder);
        productionOrderRepository.save(productionOrder);

        // Publish events
        domainEventPublisher.publishAll(reworkOrder.getDomainEvents());
        reworkOrder.clearDomainEvents();

        domainEventPublisher.publishAll(productionOrder.getDomainEvents());
        productionOrder.clearDomainEvents();
    }
}
