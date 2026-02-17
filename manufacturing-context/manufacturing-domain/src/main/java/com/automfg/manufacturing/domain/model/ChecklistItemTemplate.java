package com.automfg.manufacturing.domain.model;

import java.util.Objects;

/**
 * Value object representing a checklist item template used to create inspection items.
 * This is part of the manufacturing domain's anti-corruption layer — it does not depend
 * on the vehicle-config context directly.
 */
public record ChecklistItemTemplate(String description, boolean safetyRelated) {
    public ChecklistItemTemplate {
        Objects.requireNonNull(description, "Description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
    }
}
