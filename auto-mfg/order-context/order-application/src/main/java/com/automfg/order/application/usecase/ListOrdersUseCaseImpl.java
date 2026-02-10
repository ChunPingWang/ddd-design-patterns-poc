package com.automfg.order.application.usecase;

import com.automfg.order.domain.port.OrderRepository;

import java.util.List;

public class ListOrdersUseCaseImpl implements ListOrdersUseCase {

    private final OrderRepository orderRepository;

    public ListOrdersUseCaseImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderSummary> execute(ListOrdersQuery query) {
        if (query.dealerId() == null) {
            return List.of();
        }

        String status = query.status() != null ? query.status() : "PLACED";

        return orderRepository.findByDealerIdAndStatus(query.dealerId(), status)
            .stream()
            .map(order -> new OrderSummary(
                order.getId().value(), order.getOrderNumber().value(),
                order.getDealerId(), order.getVehicleModelCode(),
                order.getStatus().name(), order.getEstimatedDeliveryDate(),
                order.getPriceQuote()
            ))
            .toList();
    }
}
