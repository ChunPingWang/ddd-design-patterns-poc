package com.automfg.order.infrastructure.persistence;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.port.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    public JpaOrderRepositoryAdapter(OrderJpaRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toJpaEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByDealerIdAndStatus(String dealerId, String status) {
        return jpaRepository.findByDealerIdAndStatus(dealerId, status)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countByDealerIdAndVehicleModelCodeAndStatus(String dealerId,
                                                            String vehicleModelCode,
                                                            String status) {
        return jpaRepository.countByDealerIdAndVehicleModelCodeAndStatus(
                dealerId, vehicleModelCode, status);
    }
}
