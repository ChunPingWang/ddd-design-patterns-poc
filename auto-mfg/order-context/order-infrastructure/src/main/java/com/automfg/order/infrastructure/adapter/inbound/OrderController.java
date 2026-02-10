package com.automfg.order.infrastructure.adapter.inbound;

import com.automfg.order.application.usecase.ChangeOrderUseCase;
import com.automfg.order.application.usecase.GetOrderUseCase;
import com.automfg.order.application.usecase.ListOrdersUseCase;
import com.automfg.order.application.usecase.PlaceOrderUseCase;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    // Command use cases
    private final PlaceOrderUseCase placeOrderUseCase;
    private final ChangeOrderUseCase changeOrderUseCase;

    // Query use cases (CQRS read path)
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           ChangeOrderUseCase changeOrderUseCase,
                           GetOrderUseCase getOrderUseCase,
                           ListOrdersUseCase listOrdersUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.changeOrderUseCase = changeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
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

    public record ErrorResponse(String message) {}

    // --- Command Endpoints ---

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

    // --- Query Endpoints (CQRS read path) ---

    @GetMapping
    public ResponseEntity<List<ListOrdersUseCase.OrderSummary>> listOrders(
            @RequestParam(required = false) String dealerId,
            @RequestParam(required = false) String status) {
        List<ListOrdersUseCase.OrderSummary> result = listOrdersUseCase.execute(
                new ListOrdersUseCase.ListOrdersQuery(dealerId, status));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId) {
        try {
            GetOrderUseCase.OrderDetail result = getOrderUseCase.execute(
                    new GetOrderUseCase.GetOrderQuery(orderId));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
