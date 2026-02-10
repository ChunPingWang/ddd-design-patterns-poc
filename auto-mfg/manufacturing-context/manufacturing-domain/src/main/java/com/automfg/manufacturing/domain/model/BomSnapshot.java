package com.automfg.manufacturing.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BomSnapshot {

    private final List<BomLineItem> lineItems;
    private final LocalDateTime snapshotDate;

    public BomSnapshot(List<BomLineItem> lineItems) {
        Objects.requireNonNull(lineItems, "Line items must not be null");
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("BOM must not be empty");
        }
        this.lineItems = List.copyOf(lineItems);
        this.snapshotDate = LocalDateTime.now();
    }

    public List<BomLineItem> getLineItems() {
        return lineItems;
    }

    public LocalDateTime getSnapshotDate() {
        return snapshotDate;
    }

    public boolean isFullyAvailable() {
        return lineItems.stream().allMatch(BomLineItem::available);
    }

    public List<BomLineItem> getMissingMaterials() {
        return lineItems.stream()
                .filter(item -> !item.available())
                .toList();
    }
}
