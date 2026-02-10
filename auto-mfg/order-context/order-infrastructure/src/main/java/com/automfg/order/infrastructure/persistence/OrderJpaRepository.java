package com.automfg.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByDealerIdAndStatus(String dealerId, String status);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.dealerId = :dealerId " +
           "AND o.vehicleModelCode = :vehicleModelCode AND o.status = :status")
    int countByDealerIdAndVehicleModelCodeAndStatus(
            @Param("dealerId") String dealerId,
            @Param("vehicleModelCode") String vehicleModelCode,
            @Param("status") String status);
}
