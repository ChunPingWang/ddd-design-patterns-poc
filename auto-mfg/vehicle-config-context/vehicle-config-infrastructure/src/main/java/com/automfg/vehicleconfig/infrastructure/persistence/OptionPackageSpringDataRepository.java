package com.automfg.vehicleconfig.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionPackageSpringDataRepository extends JpaRepository<OptionPackageJpaEntity, UUID> {

    List<OptionPackageJpaEntity> findByVehicleConfigurationId(UUID vehicleConfigurationId);
}
