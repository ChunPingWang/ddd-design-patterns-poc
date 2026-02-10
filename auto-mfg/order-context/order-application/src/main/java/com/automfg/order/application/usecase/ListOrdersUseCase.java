package com.automfg.order.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ListOrdersUseCase extends QueryUseCase {

    record ListOrdersQuery(String dealerId, String status) {}

    record OrderSummary(
        UUID id, String orderNumber, String dealerId,
        String vehicleModelCode, String status,
        LocalDate estimatedDeliveryDate, BigDecimal priceQuote
    ) {}

    List<OrderSummary> execute(ListOrdersQuery query);
}
