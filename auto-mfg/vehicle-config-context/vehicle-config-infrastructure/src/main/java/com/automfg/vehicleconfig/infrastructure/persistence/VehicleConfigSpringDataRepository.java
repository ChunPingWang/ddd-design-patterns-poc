package com.automfg.vehicleconfig.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleConfigSpringDataRepository extends JpaRepository<VehicleConfigJpaEntity, UUID> {

    Optional<VehicleConfigJpaEntity> findByModelCode(String modelCode);
}
