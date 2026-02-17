package com.automfg.vehicleconfig.domain.port;

import com.automfg.vehicleconfig.domain.model.InspectionChecklistEntry;
import com.automfg.vehicleconfig.domain.model.VehicleConfiguration;

import java.util.List;
import java.util.Optional;

public interface VehicleConfigurationRepository {

    Optional<VehicleConfiguration> findByModelCode(String modelCode);

    List<InspectionChecklistEntry> findChecklistByModelCode(String modelCode);
}
