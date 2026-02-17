package com.automfg.order.domain.event;

import com.automfg.shared.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public class OrderPlacedEvent extends DomainEvent {

    private final UUID orderId;
    private final String orderNumber;
    private final String dealerId;
    private final String vehicleModelCode;
    private final String colorCode;
    private final List<String> optionPackageCodes;

    public OrderPlacedEvent(UUID orderId, String orderNumber, String dealerId,
                            String vehicleModelCode, String colorCode,
                            List<String> optionPackageCodes) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.dealerId = dealerId;
        this.vehicleModelCode = vehicleModelCode;
        this.colorCode = colorCode;
        this.optionPackageCodes = List.copyOf(optionPackageCodes);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getDealerId() {
        return dealerId;
    }

    public String getVehicleModelCode() {
        return vehicleModelCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public List<String> getOptionPackageCodes() {
        return optionPackageCodes;
    }
}
