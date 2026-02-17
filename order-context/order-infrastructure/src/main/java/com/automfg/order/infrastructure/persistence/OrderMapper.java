package com.automfg.order.infrastructure.persistence;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.model.OrderNumber;
import com.automfg.order.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public Order toDomain(OrderJpaEntity entity) {
        List<String> optionCodes = entity.getOptionPackageCodes() != null
                && !entity.getOptionPackageCodes().isBlank()
                ? Arrays.asList(entity.getOptionPackageCodes().split(","))
                : Collections.emptyList();

        return Order.reconstitute(
                new OrderId(entity.getId()),
                new OrderNumber(entity.getOrderNumber()),
                entity.getDealerId(),
                entity.getVehicleModelCode(),
                entity.getColorCode(),
                optionCodes,
                OrderStatus.valueOf(entity.getStatus()),
                entity.getEstimatedDeliveryDate(),
                entity.getPriceQuote(),
                entity.getChangeCount(),
                entity.getOrderDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId().value());
        entity.setOrderNumber(order.getOrderNumber().value());
        entity.setDealerId(order.getDealerId());
        entity.setVehicleModelCode(order.getVehicleModelCode());
        entity.setColorCode(order.getColorCode());
        entity.setOptionPackageCodes(String.join(",", order.getOptionPackageCodes()));
        entity.setStatus(order.getStatus().name());
        entity.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        entity.setPriceQuote(order.getPriceQuote());
        entity.setChangeCount(order.getChangeCount());
        entity.setOrderDate(order.getOrderDate());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        return entity;
    }
}
