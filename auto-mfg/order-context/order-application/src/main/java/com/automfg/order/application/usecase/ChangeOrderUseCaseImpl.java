package com.automfg.order.application.usecase;

import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.port.OrderRepository;
import com.automfg.order.domain.port.VehicleConfigGateway;
import com.automfg.shared.domain.DomainEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ChangeOrderUseCaseImpl implements ChangeOrderUseCase {

    private final OrderRepository orderRepository;
    private final VehicleConfigGateway vehicleConfigGateway;
    private final DomainEventPublisher domainEventPublisher;
    private final PlaceOrderUseCase placeOrderUseCase;

    public ChangeOrderUseCaseImpl(OrderRepository orderRepository,
                                   VehicleConfigGateway vehicleConfigGateway,
                                   DomainEventPublisher domainEventPublisher,
                                   PlaceOrderUseCase placeOrderUseCase) {
        this.orderRepository = orderRepository;
        this.vehicleConfigGateway = vehicleConfigGateway;
        this.domainEventPublisher = domainEventPublisher;
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @Override
    public ChangeOrderResult execute(ChangeOrderCommand command) {
        Order existingOrder = orderRepository.findById(new OrderId(command.orderId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + command.orderId()));

        // BR-14: If vehicle model code changes, cancel existing and place new order
        if (command.newVehicleModelCode() != null
                && !command.newVehicleModelCode().equals(existingOrder.getVehicleModelCode())) {
            return handleModelChange(existingOrder, command);
        }

        // Otherwise, change configuration on the existing order
        return handleConfigurationChange(existingOrder, command);
    }

    private ChangeOrderResult handleModelChange(Order existingOrder, ChangeOrderCommand command) {
        // Cancel the existing order
        existingOrder.cancel();
        orderRepository.save(existingOrder);
        domainEventPublisher.publishAll(existingOrder.getDomainEvents());
        existingOrder.clearDomainEvents();

        // Place a new order with the new model
        String colorCode = command.newColorCode() != null
                ? command.newColorCode()
                : existingOrder.getColorCode();
        List<String> optionCodes = command.newOptionPackageCodes() != null
                ? command.newOptionPackageCodes()
                : existingOrder.getOptionPackageCodes();

        PlaceOrderUseCase.PlaceOrderResult newOrderResult = placeOrderUseCase.execute(
                new PlaceOrderUseCase.PlaceOrderCommand(
                        existingOrder.getDealerId(),
                        command.newVehicleModelCode(),
                        colorCode,
                        optionCodes
                )
        );

        return new ChangeOrderResult(
                existingOrder.getId().value(),
                existingOrder.getOrderNumber().value(),
                colorCode,
                optionCodes,
                newOrderResult.priceQuote(),
                0,
                newOrderResult.orderId()
        );
    }

    private ChangeOrderResult handleConfigurationChange(Order existingOrder,
                                                         ChangeOrderCommand command) {
        String newColorCode = command.newColorCode() != null
                ? command.newColorCode()
                : existingOrder.getColorCode();
        List<String> newOptionCodes = command.newOptionPackageCodes() != null
                ? command.newOptionPackageCodes()
                : existingOrder.getOptionPackageCodes();

        // Validate the new configuration
        VehicleConfigGateway.ValidationResult validationResult =
                vehicleConfigGateway.validateConfiguration(
                        existingOrder.getVehicleModelCode(),
                        newColorCode,
                        newOptionCodes
                );
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(
                    "Invalid vehicle configuration: "
                            + String.join(", ", validationResult.violations()));
        }

        // Calculate new price
        BigDecimal newPrice = vehicleConfigGateway.calculatePrice(
                existingOrder.getVehicleModelCode(), newOptionCodes);

        // Apply change
        existingOrder.changeConfiguration(newColorCode, newOptionCodes, newPrice);

        // Save and publish events
        orderRepository.save(existingOrder);
        domainEventPublisher.publishAll(existingOrder.getDomainEvents());
        existingOrder.clearDomainEvents();

        return new ChangeOrderResult(
                existingOrder.getId().value(),
                existingOrder.getOrderNumber().value(),
                existingOrder.getColorCode(),
                existingOrder.getOptionPackageCodes(),
                existingOrder.getPriceQuote(),
                existingOrder.getChangeCount(),
                null
        );
    }
}
