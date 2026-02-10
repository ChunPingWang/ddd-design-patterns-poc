package com.automfg.vehicleconfig.domain.model;

import java.util.Objects;
import java.util.UUID;

public record VehicleConfigurationId(UUID value) {
    public VehicleConfigurationId {
        Objects.requireNonNull(value, "VehicleConfigurationId must not be null");
    }
}
