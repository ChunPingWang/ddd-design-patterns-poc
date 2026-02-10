package com.automfg.order.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface ChangeOrderUseCase extends CommandUseCase {

    record ChangeOrderCommand(UUID orderId, String newColorCode,
                               List<String> newOptionPackageCodes,
                               String newVehicleModelCode) {}

    record ChangeOrderResult(UUID orderId, String orderNumber, String colorCode,
                              List<String> optionPackageCodes, BigDecimal priceQuote,
                              int changeCount, UUID newOrderId) {}

    ChangeOrderResult execute(ChangeOrderCommand command);
}
