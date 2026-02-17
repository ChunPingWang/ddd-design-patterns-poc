package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProductionOrderRepositoryAdapter implements ProductionOrderRepository {

    private final ProductionOrderJpaRepository jpaRepository;
    private final ProductionOrderMapper mapper;

    public JpaProductionOrderRepositoryAdapter(ProductionOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = new ProductionOrderMapper();
    }

    @Override
    public ProductionOrder save(ProductionOrder order) {
        ProductionOrderJpaEntity entity = mapper.toJpaEntity(order);
        ProductionOrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProductionOrder> findById(ProductionOrderId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySourceOrderId(UUID sourceOrderId) {
        return jpaRepository.existsBySourceOrderId(sourceOrderId);
    }
}
