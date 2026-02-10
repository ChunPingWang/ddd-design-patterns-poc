package com.automfg.order.domain.port;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByDealerIdAndStatus(String dealerId, String status);

    int countByDealerIdAndVehicleModelCodeAndStatus(String dealerId, String vehicleModelCode, String status);
}
