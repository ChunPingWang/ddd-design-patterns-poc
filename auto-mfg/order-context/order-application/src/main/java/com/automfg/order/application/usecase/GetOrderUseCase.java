package com.automfg.order.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GetOrderUseCase extends QueryUseCase {

    record GetOrderQuery(UUID orderId) {}

    record OrderDetail(
        UUID id, String orderNumber, String dealerId,
        String vehicleModelCode, String colorCode,
        List<String> optionPackageCodes, String status,
        LocalDate estimatedDeliveryDate, BigDecimal priceQuote,
        int changeCount, LocalDateTime orderDate,
        LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    OrderDetail execute(GetOrderQuery query);
}
