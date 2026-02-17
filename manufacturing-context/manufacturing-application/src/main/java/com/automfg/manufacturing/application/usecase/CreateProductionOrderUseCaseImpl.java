package com.automfg.manufacturing.application.usecase;

import com.automfg.manufacturing.domain.model.AssemblyStepTemplate;
import com.automfg.manufacturing.domain.model.BomSnapshot;
import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;
import com.automfg.manufacturing.domain.model.ProductionOrderNumber;
import com.automfg.manufacturing.domain.model.VIN;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import com.automfg.manufacturing.domain.service.BomExpansionService;
import com.automfg.shared.domain.DomainEventPublisher;

import java.time.YearMonth;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateProductionOrderUseCaseImpl implements CreateProductionOrderUseCase {

    private final ProductionOrderRepository productionOrderRepository;
    private final BomExpansionService bomExpansionService;
    private final DomainEventPublisher domainEventPublisher;

    // Simple sequence counter for PoC (in production, this would come from a DB sequence)
    private static final AtomicInteger SEQUENCE_COUNTER = new AtomicInteger(1);

    // VIN-valid characters (excluding I, O, Q)
    private static final String VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    // Hardcoded assembly step templates for PoC
    private static final List<AssemblyStepTemplate> DEFAULT_ASSEMBLY_TEMPLATES = List.of(
        new AssemblyStepTemplate("WS-BODY", 1, "Body-in-white welding and frame assembly", 60),
        new AssemblyStepTemplate("WS-PAINT", 2, "Surface treatment and paint application", 45),
        new AssemblyStepTemplate("WS-TRIM", 3, "Interior trim and dashboard installation", 30),
        new AssemblyStepTemplate("WS-MECH", 4, "Powertrain and mechanical assembly", 90),
        new AssemblyStepTemplate("WS-FINAL", 5, "Final assembly and pre-delivery inspection", 20)
    );

    public CreateProductionOrderUseCaseImpl(ProductionOrderRepository productionOrderRepository,
                                            BomExpansionService bomExpansionService,
                                            DomainEventPublisher domainEventPublisher) {
        this.productionOrderRepository = productionOrderRepository;
        this.bomExpansionService = bomExpansionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public CreateProductionOrderResult execute(CreateProductionOrderCommand command) {
        // 1. Idempotency check
        if (productionOrderRepository.existsBySourceOrderId(command.sourceOrderId())) {
            return findExistingResult(command.sourceOrderId());
        }

        // 2. Generate VIN (random valid 17-char for PoC)
        VIN vin = generateVin();

        // 3. Generate ProductionOrderNumber (PO-SH-YYYYMM-NNNNN)
        ProductionOrderNumber orderNumber = generateOrderNumber();

        // 4. Expand BOM via BomExpansionService
        BomSnapshot bomSnapshot = bomExpansionService.expandBom(
            command.vehicleModelCode(), command.optionPackageCodes());

        // 5. Get assembly step templates (hardcoded for PoC)
        List<AssemblyStepTemplate> templates = DEFAULT_ASSEMBLY_TEMPLATES;

        // 6. Create ProductionOrder via factory
        ProductionOrderId orderId = new ProductionOrderId(UUID.randomUUID());
        ProductionOrder order = ProductionOrder.create(
            orderId, orderNumber, command.sourceOrderId(), vin, bomSnapshot, templates);

        // 7. Save and publish events
        productionOrderRepository.save(order);
        domainEventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return new CreateProductionOrderResult(
            orderId.value(),
            orderNumber.value(),
            vin.value(),
            order.getStatus().name()
        );
    }

    private CreateProductionOrderResult findExistingResult(UUID sourceOrderId) {
        // In a full implementation, we'd look up by sourceOrderId.
        // For PoC, return a placeholder indicating idempotency triggered.
        return new CreateProductionOrderResult(
            null, null, null, "ALREADY_EXISTS"
        );
    }

    private VIN generateVin() {
        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < 17; i++) {
            sb.append(VIN_CHARS.charAt(RANDOM.nextInt(VIN_CHARS.length())));
        }
        return new VIN(sb.toString());
    }

    private ProductionOrderNumber generateOrderNumber() {
        YearMonth now = YearMonth.now();
        String yearMonth = String.format("%d%02d", now.getYear(), now.getMonthValue());
        String sequence = String.format("%05d", SEQUENCE_COUNTER.getAndIncrement());
        return new ProductionOrderNumber("PO-SH-" + yearMonth + "-" + sequence);
    }
}
