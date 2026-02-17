package com.automfg.order.application.usecase;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.model.OrderNumber;
import com.automfg.order.domain.port.OrderRepository;
import com.automfg.order.domain.port.VehicleConfigGateway;
import com.automfg.shared.domain.DomainEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private static final int MAX_ORDERS_PER_DEALER_MODEL = 50;
    private static final int DELIVERY_LEAD_DAYS = 45;

    private final OrderRepository orderRepository;
    private final VehicleConfigGateway vehicleConfigGateway;
    private final DomainEventPublisher domainEventPublisher;

    public PlaceOrderUseCaseImpl(OrderRepository orderRepository,
                                  VehicleConfigGateway vehicleConfigGateway,
                                  DomainEventPublisher domainEventPublisher) {
        this.orderRepository = orderRepository;
        this.vehicleConfigGateway = vehicleConfigGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public PlaceOrderResult execute(PlaceOrderCommand command) {
        // 1. Validate configuration via VehicleConfigGateway
        VehicleConfigGateway.ValidationResult validationResult =
                vehicleConfigGateway.validateConfiguration(
                        command.vehicleModelCode(),
                        command.colorCode(),
                        command.optionPackageCodes()
                );
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(
                    "Invalid vehicle configuration: " + String.join(", ", validationResult.violations()));
        }

        // 2. BR-01: Check 50-order limit per dealer per model (active orders only)
        int activeOrderCount = orderRepository.countByDealerIdAndVehicleModelCodeAndStatus(
                command.dealerId(), command.vehicleModelCode(), "PLACED");
        activeOrderCount += orderRepository.countByDealerIdAndVehicleModelCodeAndStatus(
                command.dealerId(), command.vehicleModelCode(), "SCHEDULED");
        activeOrderCount += orderRepository.countByDealerIdAndVehicleModelCodeAndStatus(
                command.dealerId(), command.vehicleModelCode(), "IN_PRODUCTION");

        if (activeOrderCount >= MAX_ORDERS_PER_DEALER_MODEL) {
            throw new IllegalStateException(
                    "Dealer " + command.dealerId() + " has reached the maximum of "
                            + MAX_ORDERS_PER_DEALER_MODEL + " active orders for model "
                            + command.vehicleModelCode());
        }

        // 3. Generate OrderNumber (format: ORD-YYYYMM-NNNNN)
        OrderNumber orderNumber = generateOrderNumber();

        // 4. Calculate delivery date (order date + 45 days)
        LocalDate estimatedDeliveryDate = LocalDate.now().plusDays(DELIVERY_LEAD_DAYS);

        // 5. Calculate price via VehicleConfigGateway
        BigDecimal priceQuote = vehicleConfigGateway.calculatePrice(
                command.vehicleModelCode(), command.optionPackageCodes());

        // 6. Create Order via factory method
        OrderId orderId = OrderId.generate();
        Order order = Order.place(
                orderId, orderNumber, command.dealerId(),
                command.vehicleModelCode(), command.colorCode(),
                command.optionPackageCodes(),
                estimatedDeliveryDate, priceQuote
        );

        // 7. Save order
        orderRepository.save(order);

        // 8. Publish domain events
        domainEventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return new PlaceOrderResult(
                orderId.value(),
                orderNumber.value(),
                order.getEstimatedDeliveryDate(),
                priceQuote
        );
    }

    private OrderNumber generateOrderNumber() {
        YearMonth now = YearMonth.now();
        String yearMonth = String.format("%04d%02d", now.getYear(), now.getMonthValue());
        int sequence = ThreadLocalRandom.current().nextInt(1, 100000);
        String formatted = String.format("ORD-%s-%05d", yearMonth, sequence);
        return new OrderNumber(formatted);
    }
}
