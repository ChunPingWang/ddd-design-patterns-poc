package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.ReworkOrder;
import com.automfg.manufacturing.domain.port.ReworkOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaReworkOrderRepositoryAdapter implements ReworkOrderRepository {

    private final ReworkOrderJpaRepository jpaRepository;

    public JpaReworkOrderRepositoryAdapter(ReworkOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReworkOrder save(ReworkOrder order) {
        ReworkOrderJpaEntity entity = ReworkOrderMapper.toJpaEntity(order);
        ReworkOrderJpaEntity saved = jpaRepository.save(entity);
        return ReworkOrderMapper.toDomain(saved);
    }

    @Override
    public Optional<ReworkOrder> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(ReworkOrderMapper::toDomain);
    }
}
