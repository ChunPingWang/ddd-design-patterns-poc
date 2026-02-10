package com.automfg.order.domain.model;

import com.automfg.order.domain.event.OrderChangedEvent;
import com.automfg.order.domain.event.OrderPlacedEvent;
import com.automfg.shared.domain.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order extends AggregateRoot {

    private static final int MAX_CHANGES = 3;
    private static final int DELIVERY_LEAD_DAYS = 45;

    private final OrderId id;
    private final OrderNumber orderNumber;
    private final String dealerId;
    private final String vehicleModelCode;
    private String colorCode;
    private List<String> optionPackageCodes;
    private OrderStatus status;
    private LocalDate estimatedDeliveryDate;
    private BigDecimal priceQuote;
    private int changeCount;
    private final LocalDateTime orderDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Order(OrderId id, OrderNumber orderNumber, String dealerId,
                  String vehicleModelCode, String colorCode,
                  List<String> optionPackageCodes, OrderStatus status,
                  LocalDate estimatedDeliveryDate, BigDecimal priceQuote,
                  int changeCount, LocalDateTime orderDate,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.orderNumber = Objects.requireNonNull(orderNumber);
        this.dealerId = Objects.requireNonNull(dealerId);
        this.vehicleModelCode = Objects.requireNonNull(vehicleModelCode);
        this.colorCode = Objects.requireNonNull(colorCode);
        this.optionPackageCodes = new ArrayList<>(optionPackageCodes);
        this.status = status;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.priceQuote = Objects.requireNonNull(priceQuote);
        this.changeCount = changeCount;
        this.orderDate = orderDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method to place a new order.
     * BR-03: estimatedDeliveryDate = orderDate + 45 days.
     */
    public static Order place(OrderId id, OrderNumber orderNumber, String dealerId,
                              String vehicleModelCode, String colorCode,
                              List<String> optionPackageCodes,
                              LocalDate estimatedDeliveryDate, BigDecimal priceQuote) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate orderDateLocal = now.toLocalDate();

        // BR-03: enforce estimated delivery date is at least orderDate + 45 days
        LocalDate minimumDeliveryDate = orderDateLocal.plusDays(DELIVERY_LEAD_DAYS);
        if (estimatedDeliveryDate.isBefore(minimumDeliveryDate)) {
            estimatedDeliveryDate = minimumDeliveryDate;
        }

        Order order = new Order(
                id, orderNumber, dealerId, vehicleModelCode, colorCode,
                optionPackageCodes, OrderStatus.PLACED, estimatedDeliveryDate,
                priceQuote, 0, now, now, now
        );

        order.registerEvent(new OrderPlacedEvent(
                id.value(),
                orderNumber.value(),
                dealerId,
                vehicleModelCode,
                colorCode,
                List.copyOf(optionPackageCodes)
        ));

        return order;
    }

    /**
     * Reconstitute an Order from persistence without raising domain events.
     */
    public static Order reconstitute(OrderId id, OrderNumber orderNumber, String dealerId,
                                     String vehicleModelCode, String colorCode,
                                     List<String> optionPackageCodes, OrderStatus status,
                                     LocalDate estimatedDeliveryDate, BigDecimal priceQuote,
                                     int changeCount, LocalDateTime orderDate,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, orderNumber, dealerId, vehicleModelCode, colorCode,
                optionPackageCodes, status, estimatedDeliveryDate, priceQuote,
                changeCount, orderDate, createdAt, updatedAt);
    }

    public void markScheduled() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Cannot mark order as SCHEDULED from status: " + status);
        }
        this.status = OrderStatus.SCHEDULED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markInProduction() {
        if (status != OrderStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot mark order as IN_PRODUCTION from status: " + status);
        }
        this.status = OrderStatus.IN_PRODUCTION;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        if (status != OrderStatus.IN_PRODUCTION) {
            throw new IllegalStateException(
                    "Cannot mark order as COMPLETED from status: " + status);
        }
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Change configuration of an existing order.
     * BR-15: maximum 3 configuration changes allowed.
     */
    public void changeConfiguration(String newColorCode, List<String> newOptionPackageCodes,
                                    BigDecimal newPriceQuote) {
        if (status != OrderStatus.PLACED && status != OrderStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot change configuration when order status is: " + status);
        }
        if (changeCount >= MAX_CHANGES) {
            throw new IllegalStateException(
                    "Maximum number of configuration changes (" + MAX_CHANGES + ") has been reached");
        }

        this.colorCode = Objects.requireNonNull(newColorCode);
        this.optionPackageCodes = new ArrayList<>(newOptionPackageCodes);
        this.priceQuote = Objects.requireNonNull(newPriceQuote);
        this.changeCount++;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new OrderChangedEvent(
                id.value(),
                newColorCode,
                List.copyOf(newOptionPackageCodes)
        ));
    }

    public void cancel() {
        if (status != OrderStatus.PLACED && status != OrderStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Cannot cancel order when status is: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters

    public OrderId getId() {
        return id;
    }

    public OrderNumber getOrderNumber() {
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
        return Collections.unmodifiableList(optionPackageCodes);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public BigDecimal getPriceQuote() {
        return priceQuote;
    }

    public int getChangeCount() {
        return changeCount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
