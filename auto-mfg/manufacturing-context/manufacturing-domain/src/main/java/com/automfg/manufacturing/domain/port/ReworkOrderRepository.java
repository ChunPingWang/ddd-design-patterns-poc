package com.automfg.manufacturing.domain.port;

import com.automfg.manufacturing.domain.model.ReworkOrder;

import java.util.Optional;
import java.util.UUID;

public interface ReworkOrderRepository {
    ReworkOrder save(ReworkOrder order);
    Optional<ReworkOrder> findById(UUID id);
}
