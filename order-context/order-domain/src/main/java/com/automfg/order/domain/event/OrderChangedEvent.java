package com.automfg.order.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public class OrderChangedEvent extends DomainEvent {

    private final UUID orderId;
    private final String newColorCode;
    private final List<String> newOptionPackageCodes;

    public OrderChangedEvent(UUID orderId, String newColorCode, List<String> newOptionPackageCodes) {
        super();
        this.orderId = orderId;
        this.newColorCode = newColorCode;
        this.newOptionPackageCodes = List.copyOf(newOptionPackageCodes);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getNewColorCode() {
        return newColorCode;
    }

    public List<String> getNewOptionPackageCodes() {
        return newOptionPackageCodes;
    }
}
