package com.automfg.manufacturing.domain.model;

import java.util.Objects;

public record BomLineItem(
    String partNumber,
    String partDescription,
    int quantityRequired,
    String unitOfMeasure,
    boolean available
) {
    public BomLineItem {
        Objects.requireNonNull(partNumber, "Part number required");
        Objects.requireNonNull(partDescription, "Part description required");
        if (quantityRequired <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(unitOfMeasure, "Unit of measure required");
    }
}
