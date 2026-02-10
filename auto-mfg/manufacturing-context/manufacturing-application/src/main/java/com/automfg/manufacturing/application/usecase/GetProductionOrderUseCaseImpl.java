package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.application.port.ProductionOrderQueryPort;

public class GetProductionOrderUseCaseImpl implements GetProductionOrderUseCase {

    private final ProductionOrderQueryPort queryPort;

    public GetProductionOrderUseCaseImpl(ProductionOrderQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public ProductionOrderDetail execute(GetProductionOrderQuery query) {
        return queryPort.findById(query.productionOrderId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Production order not found: " + query.productionOrderId()));
    }
}
