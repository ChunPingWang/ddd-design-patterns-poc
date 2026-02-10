package com.automfg.order.infrastructure.adapter.inbound;

import com.automfg.order.application.usecase.ChangeOrderUseCase;
import com.automfg.order.application.usecase.PlaceOrderUseCase;
import com.automfg.order.domain.model.Order;
import com.automfg.order.domain.model.OrderId;
import com.automfg.order.domain.port.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final ChangeOrderUseCase changeOrderUseCase;
    private final OrderRepository orderRepository;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           ChangeOrderUseCase changeOrderUseCase,
                           OrderRepository orderRepository) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.changeOrderUseCase = changeOrderUseCase;
        this.orderRepository = orderRepository;
    }

    // --- Request/Response DTOs ---

    public record PlaceOrderRequest(String dealerId, String vehicleModelCode,
                                     String colorCode, List<String> optionPackageCodes) {}

    public record PlaceOrderResponse(UUID orderId, String orderNumber,
                                      LocalDate estimatedDeliveryDate, BigDecimal priceQuote) {}

    public record ChangeOrderRequest(String newColorCode, List<String> newOptionPackageCodes,
                                      String newVehicleModelCode) {}

    public record ChangeOrderResponse(UUID orderId, String orderNumber, String colorCode,
                                       List<String> optionPackageCodes, BigDecimal priceQuote,
                                       int changeCount, UUID newOrderId) {}

    public record OrderResponse(UUID id, String orderNumber, String dealerId,
                                 String vehicleModelCode, String colorCode,
                                 List<String> optionPackageCodes, String status,
                                 LocalDate estimatedDeliveryDate, BigDecimal priceQuote,
                                 int changeCount, LocalDateTime orderDate,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ErrorResponse(String message) {}

    // --- Endpoints ---

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        try {
            PlaceOrderUseCase.PlaceOrderResult result = placeOrderUseCase.execute(
                    new PlaceOrderUseCase.PlaceOrderCommand(
                            request.dealerId(),
                            request.vehicleModelCode(),
                            request.colorCode(),
                            request.optionPackageCodes()
                    )
            );

            PlaceOrderResponse response = new PlaceOrderResponse(
                    result.orderId(),
                    result.orderNumber(),
                    result.estimatedDeliveryDate(),
                    result.priceQuote()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(required = false) String dealerId,
            @RequestParam(required = false) String status) {
        List<Order> orders;
        if (dealerId != null && status != null) {
            orders = orderRepository.findByDealerIdAndStatus(dealerId, status);
        } else {
            // Fallback: if no filters, we could return empty or all.
            // For safety, return empty when no filters provided.
            orders = dealerId != null
                    ? orderRepository.findByDealerIdAndStatus(dealerId, "PLACED")
                    : List.of();
        }

        List<OrderResponse> responses = orders.stream()
                .map(this::toOrderResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId) {
        return orderRepository.findById(new OrderId(orderId))
                .map(order -> ResponseEntity.ok(toOrderResponse(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/changes")
    public ResponseEntity<?> changeOrder(@PathVariable UUID orderId,
                                          @RequestBody ChangeOrderRequest request) {
        try {
            ChangeOrderUseCase.ChangeOrderResult result = changeOrderUseCase.execute(
                    new ChangeOrderUseCase.ChangeOrderCommand(
                            orderId,
                            request.newColorCode(),
                            request.newOptionPackageCodes(),
                            request.newVehicleModelCode()
                    )
            );

            ChangeOrderResponse response = new ChangeOrderResponse(
                    result.orderId(),
                    result.orderNumber(),
                    result.colorCode(),
                    result.optionPackageCodes(),
                    result.priceQuote(),
                    result.changeCount(),
                    result.newOrderId()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId().value(),
                order.getOrderNumber().value(),
                order.getDealerId(),
                order.getVehicleModelCode(),
                order.getColorCode(),
                order.getOptionPackageCodes(),
                order.getStatus().name(),
                order.getEstimatedDeliveryDate(),
                order.getPriceQuote(),
                order.getChangeCount(),
                order.getOrderDate(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
