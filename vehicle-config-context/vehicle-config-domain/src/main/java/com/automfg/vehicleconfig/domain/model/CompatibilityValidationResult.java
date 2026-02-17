package com.automfg.vehicleconfig.domain.model;

import java.util.Collections;
import java.util.List;

public record CompatibilityValidationResult(boolean valid, List<String> violations) {

    public static CompatibilityValidationResult pass() {
        return new CompatibilityValidationResult(true, Collections.emptyList());
    }

    public static CompatibilityValidationResult fail(List<String> violations) {
        return new CompatibilityValidationResult(false, List.copyOf(violations));
    }
}
