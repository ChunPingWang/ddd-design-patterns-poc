package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.application.port.ProductionOrderQueryPort;

import java.util.List;

public class ListProductionOrdersUseCaseImpl implements ListProductionOrdersUseCase {

    private final ProductionOrderQueryPort queryPort;

    public ListProductionOrdersUseCaseImpl(ProductionOrderQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<ProductionOrderSummary> execute(ListProductionOrdersQuery query) {
        if (query.status() != null && !query.status().isBlank()) {
            return queryPort.findByStatus(query.status());
        }
        return queryPort.findAll();
    }
}
