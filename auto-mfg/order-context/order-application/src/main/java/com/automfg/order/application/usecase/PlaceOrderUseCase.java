package com.automfg.order.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.automfg.shared.application.CommandUseCase;

public interface PlaceOrderUseCase extends CommandUseCase {

    record PlaceOrderCommand(String dealerId, String vehicleModelCode, String colorCode,
                             List<String> optionPackageCodes) {}

    record PlaceOrderResult(UUID orderId, String orderNumber, LocalDate estimatedDeliveryDate,
                            BigDecimal priceQuote) {}

    PlaceOrderResult execute(PlaceOrderCommand command);
}
