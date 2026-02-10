package com.automfg.manufacturing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionChecklistJpaRepository extends JpaRepository<InspectionChecklistJpaEntity, UUID> {
    List<InspectionChecklistJpaEntity> findByModelCodeOrderByDisplayOrder(String modelCode);
}
