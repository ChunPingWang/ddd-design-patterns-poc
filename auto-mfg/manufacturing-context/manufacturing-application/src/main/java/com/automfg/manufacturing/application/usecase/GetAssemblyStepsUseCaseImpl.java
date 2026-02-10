package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.application.port.ProductionOrderQueryPort;

import java.util.List;

public class GetAssemblyStepsUseCaseImpl implements GetAssemblyStepsUseCase {

    private final ProductionOrderQueryPort queryPort;

    public GetAssemblyStepsUseCaseImpl(ProductionOrderQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<AssemblyStepDetail> execute(GetAssemblyStepsQuery query) {
        return queryPort.findAssemblySteps(query.productionOrderId(), query.stationCode());
    }
}
