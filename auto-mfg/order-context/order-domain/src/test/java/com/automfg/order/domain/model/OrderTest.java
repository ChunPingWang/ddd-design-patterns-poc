package com.automfg.order.domain.model;

import com.automfg.order.domain.event.OrderChangedEvent;
import com.automfg.order.domain.event.OrderPlacedEvent;
import com.automfg.shared.domain.DomainEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final OrderId ORDER_ID = OrderId.generate();
    private static final OrderNumber ORDER_NUMBER = new OrderNumber("ORD-202601-00001");
    private static final String DEALER_ID = "DEALER-001";
    private static final String MODEL_CODE = "SEDAN-LX";
    private static final String COLOR_CODE = "PEARL-WHITE";
    private static final List<String> OPTION_CODES = List.of("PKG-SPORT", "PKG-TECH");
    private static final BigDecimal PRICE = new BigDecimal("45000.00");

    private Order placeDefaultOrder() {
        return Order.place(
                ORDER_ID, ORDER_NUMBER, DEALER_ID, MODEL_CODE, COLOR_CODE,
                OPTION_CODES, LocalDate.now().plusDays(60), PRICE
        );
    }

    @Test
    void place_order_success() {
        Order order = placeDefaultOrder();

        assertThat(order.getId()).isEqualTo(ORDER_ID);
        assertThat(order.getOrderNumber()).isEqualTo(ORDER_NUMBER);
        assertThat(order.getDealerId()).isEqualTo(DEALER_ID);
        assertThat(order.getVehicleModelCode()).isEqualTo(MODEL_CODE);
        assertThat(order.getColorCode()).isEqualTo(COLOR_CODE);
        assertThat(order.getOptionPackageCodes()).containsExactlyElementsOf(OPTION_CODES);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getChangeCount()).isZero();
        // BR-03: delivery date must be at least 45 days from order date
        assertThat(order.getEstimatedDeliveryDate())
                .isAfterOrEqualTo(LocalDate.now().plusDays(45));
    }

    @Test
    void place_order_enforces_minimum_delivery_date() {
        // Provide a delivery date that is too early (only 10 days out)
        Order order = Order.place(
                ORDER_ID, ORDER_NUMBER, DEALER_ID, MODEL_CODE, COLOR_CODE,
                OPTION_CODES, LocalDate.now().plusDays(10), PRICE
        );

        // BR-03: should be bumped to at least 45 days
        assertThat(order.getEstimatedDeliveryDate())
                .isAfterOrEqualTo(LocalDate.now().plusDays(45));
    }

    @Test
    void place_order_registers_event() {
        Order order = placeDefaultOrder();

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderPlacedEvent.class);

        OrderPlacedEvent event = (OrderPlacedEvent) events.get(0);
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID.value());
        assertThat(event.getOrderNumber()).isEqualTo(ORDER_NUMBER.value());
        assertThat(event.getDealerId()).isEqualTo(DEALER_ID);
        assertThat(event.getVehicleModelCode()).isEqualTo(MODEL_CODE);
        assertThat(event.getColorCode()).isEqualTo(COLOR_CODE);
        assertThat(event.getOptionPackageCodes()).containsExactlyElementsOf(OPTION_CODES);
    }

    @Test
    void change_configuration_success() {
        Order order = placeDefaultOrder();
        String newColor = "MIDNIGHT-BLACK";
        List<String> newOptions = List.of("PKG-LUXURY");
        BigDecimal newPrice = new BigDecimal("52000.00");

        order.changeConfiguration(newColor, newOptions, newPrice);

        assertThat(order.getColorCode()).isEqualTo(newColor);
        assertThat(order.getOptionPackageCodes()).containsExactlyElementsOf(newOptions);
        assertThat(order.getPriceQuote()).isEqualTo(newPrice);
        assertThat(order.getChangeCount()).isEqualTo(1);

        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).hasSize(2); // OrderPlacedEvent + OrderChangedEvent
        assertThat(events.get(1)).isInstanceOf(OrderChangedEvent.class);
    }

    @Test
    void change_configuration_max_reached_throws() {
        Order order = placeDefaultOrder();

        // Perform 3 changes (maximum allowed)
        for (int i = 0; i < 3; i++) {
            order.changeConfiguration("COLOR-" + i, List.of("OPT-" + i),
                    new BigDecimal("50000.00"));
        }

        // BR-15: 4th change should fail
        assertThatThrownBy(() ->
                order.changeConfiguration("COLOR-X", List.of("OPT-X"),
                        new BigDecimal("50000.00"))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum number of configuration changes");
    }

    @Test
    void change_configuration_when_in_production_throws() {
        Order order = placeDefaultOrder();
        order.markScheduled();
        order.markInProduction();

        assertThatThrownBy(() ->
                order.changeConfiguration("NEW-COLOR", List.of("OPT-1"),
                        new BigDecimal("50000.00"))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change configuration");
    }

    @Test
    void cancel_order_success() {
        Order order = placeDefaultOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_when_in_production_throws() {
        Order order = placeDefaultOrder();
        order.markScheduled();
        order.markInProduction();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order");
    }

    @Test
    void mark_scheduled_success() {
        Order order = placeDefaultOrder();

        order.markScheduled();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SCHEDULED);
    }

    @Test
    void mark_in_production_success() {
        Order order = placeDefaultOrder();
        order.markScheduled();

        order.markInProduction();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }
}
