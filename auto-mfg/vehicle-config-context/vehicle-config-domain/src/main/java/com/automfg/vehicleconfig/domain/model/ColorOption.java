package com.automfg.vehicleconfig.domain.model;

import java.util.Objects;

public record ColorOption(String code, String name) {
    public ColorOption {
        Objects.requireNonNull(code, "Color code must not be null");
        Objects.requireNonNull(name, "Color name must not be null");
    }
}
