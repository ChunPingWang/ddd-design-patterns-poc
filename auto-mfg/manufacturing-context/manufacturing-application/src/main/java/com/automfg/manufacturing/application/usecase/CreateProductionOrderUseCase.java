package com.automfg.manufacturing.application.usecase;

import java.util.List;
import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface CreateProductionOrderUseCase extends CommandUseCase {

    record CreateProductionOrderCommand(
        UUID sourceOrderId,
        String vehicleModelCode,
        String colorCode,
        List<String> optionPackageCodes
    ) {}

    record CreateProductionOrderResult(
        UUID productionOrderId,
        String orderNumber,
        String vin,
        String status
    ) {}

    CreateProductionOrderResult execute(CreateProductionOrderCommand command);
}
