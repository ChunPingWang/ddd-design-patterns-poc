package com.automfg.order.application.usecase;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.port.OrderRepository;

public class GetOrderUseCaseImpl implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderDetail execute(GetOrderQuery query) {
        Order order = orderRepository.findById(new OrderId(query.orderId()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Order not found: " + query.orderId()));

        return new OrderDetail(
            order.getId().value(), order.getOrderNumber().value(),
            order.getDealerId(), order.getVehicleModelCode(),
            order.getColorCode(), order.getOptionPackageCodes(),
            order.getStatus().name(), order.getEstimatedDeliveryDate(),
            order.getPriceQuote(), order.getChangeCount(),
            order.getOrderDate(), order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}
