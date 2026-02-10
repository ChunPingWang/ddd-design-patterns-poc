package com.automfg.manufacturing.domain.port;

import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;

import java.util.Optional;
import java.util.UUID;

public interface ProductionOrderRepository {
    ProductionOrder save(ProductionOrder order);

    Optional<ProductionOrder> findById(ProductionOrderId id);

    boolean existsBySourceOrderId(UUID sourceOrderId);
}
