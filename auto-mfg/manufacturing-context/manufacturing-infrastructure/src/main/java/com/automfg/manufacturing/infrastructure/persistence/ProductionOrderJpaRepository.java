package com.automfg.manufacturing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionOrderJpaRepository extends JpaRepository<ProductionOrderJpaEntity, UUID> {

    boolean existsBySourceOrderId(UUID sourceOrderId);

    List<ProductionOrderJpaEntity> findByStatus(String status);
}
