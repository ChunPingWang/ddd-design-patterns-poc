package com.automfg.order.domain.port;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleConfigGateway {
    ValidationResult validateConfiguration(String modelCode, String colorCode, List<String> optionCodes);

    BigDecimal calculatePrice(String modelCode, List<String> optionCodes);

    record ValidationResult(boolean valid, List<String> violations) {}
}
