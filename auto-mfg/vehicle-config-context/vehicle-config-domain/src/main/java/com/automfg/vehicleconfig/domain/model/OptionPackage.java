package com.automfg.vehicleconfig.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class OptionPackage {

    private final String packageCode;
    private final String packageName;
    private final BigDecimal basePrice;

    public OptionPackage(String packageCode, String packageName, BigDecimal basePrice) {
        this.packageCode = Objects.requireNonNull(packageCode);
        this.packageName = Objects.requireNonNull(packageName);
        this.basePrice = Objects.requireNonNull(basePrice);
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
    }

    public String getPackageCode() { return packageCode; }
    public String getPackageName() { return packageName; }
    public BigDecimal getBasePrice() { return basePrice; }
}
