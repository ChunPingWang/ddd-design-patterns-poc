package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.CreateProductionOrderUseCase;
import com.automfg.shared.infrastructure.ProcessedEvent;
import com.automfg.shared.infrastructure.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Consumes OrderPlacedEvent from the order context (via Spring ApplicationEvent)
 * and triggers production order creation in the manufacturing context.
 * Idempotent: checks ProcessedEvent before processing.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String CONSUMER_NAME = "OrderEventConsumer";

    private final CreateProductionOrderUseCase createProductionOrderUseCase;
    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(CreateProductionOrderUseCase createProductionOrderUseCase,
                              ProcessedEventRepository processedEventRepository) {
        this.createProductionOrderUseCase = createProductionOrderUseCase;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Represents the OrderPlacedEvent from the order context.
     * This is a lightweight DTO/event that carries the data needed
     * to create a production order.
     */
    public static class OrderPlacedEvent {
        private final UUID eventId;
        private final UUID orderId;
        private final String vehicleModelCode;
        private final String colorCode;
        private final List<String> optionPackageCodes;

        public OrderPlacedEvent(UUID eventId, UUID orderId, String vehicleModelCode,
                                String colorCode, List<String> optionPackageCodes) {
            this.eventId = eventId;
            this.orderId = orderId;
            this.vehicleModelCode = vehicleModelCode;
            this.colorCode = colorCode;
            this.optionPackageCodes = optionPackageCodes;
        }

        public UUID getEventId() { return eventId; }
        public UUID getOrderId() { return orderId; }
        public String getVehicleModelCode() { return vehicleModelCode; }
        public String getColorCode() { return colorCode; }
        public List<String> getOptionPackageCodes() { return optionPackageCodes; }
    }

    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: eventId={}, orderId={}",
            event.getEventId(), event.getOrderId());

        // Idempotency check
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        try {
            CreateProductionOrderUseCase.CreateProductionOrderCommand command =
                new CreateProductionOrderUseCase.CreateProductionOrderCommand(
                    event.getOrderId(),
                    event.getVehicleModelCode(),
                    event.getColorCode(),
                    event.getOptionPackageCodes()
                );

            CreateProductionOrderUseCase.CreateProductionOrderResult result =
                createProductionOrderUseCase.execute(command);

            // Record that we processed this event
            processedEventRepository.save(
                new ProcessedEvent(event.getEventId(), "OrderPlacedEvent", CONSUMER_NAME));

            log.info("Created production order: id={}, status={}",
                result.productionOrderId(), result.status());

        } catch (Exception e) {
            log.error("Failed to process OrderPlacedEvent: eventId={}", event.getEventId(), e);
            throw e;
        }
    }
}
