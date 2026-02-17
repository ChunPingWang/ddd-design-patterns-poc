package com.automfg.manufacturing.domain.model;

import com.automfg.manufacturing.domain.event.AssemblyCompletedEvent;
import com.automfg.manufacturing.domain.event.AssemblyOvertimeAlertEvent;
import com.automfg.manufacturing.domain.event.MaterialShortageEvent;
import com.automfg.manufacturing.domain.event.ProductionOrderScheduledEvent;
import com.automfg.manufacturing.domain.event.ProductionStartedEvent;
import com.automfg.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionOrderTest {

    private static final ProductionOrderId ORDER_ID = new ProductionOrderId(UUID.randomUUID());
    private static final ProductionOrderNumber ORDER_NUMBER = new ProductionOrderNumber("PO-SH-202602-00001");
    private static final UUID SOURCE_ORDER_ID = UUID.randomUUID();
    private static final VIN VIN_VALUE = new VIN("1HGBH41JXMN109186");

    private static final List<AssemblyStepTemplate> TEMPLATES = List.of(
        new AssemblyStepTemplate("WS-BODY", 1, "Body welding", 60),
        new AssemblyStepTemplate("WS-PAINT", 2, "Paint application", 45),
        new AssemblyStepTemplate("WS-TRIM", 3, "Interior trim installation", 30),
        new AssemblyStepTemplate("WS-MECH", 4, "Mechanical assembly", 90),
        new AssemblyStepTemplate("WS-FINAL", 5, "Final inspection prep", 20)
    );

    private BomSnapshot allAvailableBom() {
        return new BomSnapshot(List.of(
            new BomLineItem("CHS-001", "Chassis Frame", 1, "UNIT", true),
            new BomLineItem("ENG-001", "Engine", 1, "UNIT", true),
            new BomLineItem("BAT-001", "Battery", 1, "UNIT", true)
        ));
    }

    private BomSnapshot partiallyAvailableBom() {
        return new BomSnapshot(List.of(
            new BomLineItem("CHS-001", "Chassis Frame", 1, "UNIT", true),
            new BomLineItem("ENG-001", "Engine", 1, "UNIT", false),
            new BomLineItem("BAT-001", "Battery", 1, "UNIT", false)
        ));
    }

    @Test
    @DisplayName("create with available materials schedules the production order")
    void create_with_available_materials_schedules() {
        ProductionOrder order = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            allAvailableBom(), TEMPLATES);

        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.SCHEDULED);
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(ProductionOrderScheduledEvent.class);

        ProductionOrderScheduledEvent event = (ProductionOrderScheduledEvent) order.getDomainEvents().get(0);
        assertThat(event.getProductionOrderId()).isEqualTo(ORDER_ID.value());
        assertThat(event.getOrderNumber()).isEqualTo(ORDER_NUMBER.value());
        assertThat(event.getSourceOrderId()).isEqualTo(SOURCE_ORDER_ID);
        assertThat(event.getVin()).isEqualTo(VIN_VALUE.value());
    }

    @Test
    @DisplayName("create with missing materials sets MATERIAL_PENDING status")
    void create_with_missing_materials_pending() {
        ProductionOrder order = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            partiallyAvailableBom(), TEMPLATES);

        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.MATERIAL_PENDING);
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(MaterialShortageEvent.class);

        MaterialShortageEvent event = (MaterialShortageEvent) order.getDomainEvents().get(0);
        assertThat(event.getProductionOrderId()).isEqualTo(ORDER_ID.value());
        assertThat(event.getMissingParts()).containsExactly("ENG-001", "BAT-001");
    }

    @Test
    @DisplayName("startProduction transitions SCHEDULED to IN_PRODUCTION")
    void start_production_success() {
        ProductionOrder order = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            allAvailableBom(), TEMPLATES);
        order.clearDomainEvents();

        order.startProduction("OP-001", "WS-BODY");

        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.IN_PRODUCTION);
        assertThat(order.getCurrentStationSequence()).isEqualTo(1);
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(ProductionStartedEvent.class);

        ProductionStartedEvent event = (ProductionStartedEvent) order.getDomainEvents().get(0);
        assertThat(event.getOperatorId()).isEqualTo("OP-001");
        assertThat(event.getVin()).isEqualTo(VIN_VALUE.value());
    }

    @Test
    @DisplayName("startProduction throws when status is not SCHEDULED")
    void start_production_wrong_status_throws() {
        ProductionOrder order = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            partiallyAvailableBom(), TEMPLATES);

        assertThatThrownBy(() -> order.startProduction("OP-001", "WS-BODY"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MATERIAL_PENDING");
    }

    @Test
    @DisplayName("completeAssemblyStep successfully completes a step with batch number")
    void complete_assembly_step_success() {
        ProductionOrder order = createInProductionOrder();
        AssemblyStepId stepId = order.getAssemblyProcess().getSteps().get(0).getId();

        AssemblyStepResult result = order.completeAssemblyStep(stepId, "OP-001", "BATCH-2026-001", 55);

        assertThat(result.overtimeAlert()).isFalse();
        assertThat(result.stationCompleted()).isTrue(); // only one step at station 1
        assertThat(result.assemblyCompleted()).isFalse();

        AssemblyStep completedStep = order.getAssemblyProcess().getStep(stepId);
        assertThat(completedStep.getStatus()).isEqualTo(AssemblyStepStatus.COMPLETED);
        assertThat(completedStep.getMaterialBatchId().value()).isEqualTo("BATCH-2026-001");
    }

    @Test
    @DisplayName("completeAssemblyStep triggers overtime alert when actual > 1.5x standard (BR-09)")
    void complete_assembly_step_overtime_alert() {
        ProductionOrder order = createInProductionOrder();
        // Station 1 step has standardTimeMinutes=60, so 1.5x = 90. 91 should trigger alert.
        AssemblyStepId stepId = order.getAssemblyProcess().getSteps().get(0).getId();
        order.clearDomainEvents();

        AssemblyStepResult result = order.completeAssemblyStep(stepId, "OP-001", "BATCH-001", 91);

        assertThat(result.overtimeAlert()).isTrue();
        List<DomainEvent> events = order.getDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof AssemblyOvertimeAlertEvent);

        AssemblyOvertimeAlertEvent alertEvent = events.stream()
            .filter(e -> e instanceof AssemblyOvertimeAlertEvent)
            .map(e -> (AssemblyOvertimeAlertEvent) e)
            .findFirst().orElseThrow();
        assertThat(alertEvent.getStandardMinutes()).isEqualTo(60);
        assertThat(alertEvent.getActualMinutes()).isEqualTo(91);
    }

    @Test
    @DisplayName("completing all assembly steps transitions to ASSEMBLY_COMPLETED")
    void complete_all_steps_assembly_completed() {
        ProductionOrder order = createInProductionOrder();
        List<AssemblyStep> steps = order.getAssemblyProcess().getSteps();
        order.clearDomainEvents();

        // Complete all steps in order
        for (AssemblyStep step : steps) {
            order.completeAssemblyStep(step.getId(), "OP-001", "BATCH-001",
                step.getStandardTimeMinutes());
        }

        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.ASSEMBLY_COMPLETED);
        assertThat(order.getDomainEvents())
            .anyMatch(e -> e instanceof AssemblyCompletedEvent);
    }

    @Test
    @DisplayName("isModifiable returns true for SCHEDULED and MATERIAL_PENDING, false for others")
    void is_modifiable() {
        // SCHEDULED -> modifiable
        ProductionOrder scheduledOrder = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            allAvailableBom(), TEMPLATES);
        assertThat(scheduledOrder.isModifiable()).isTrue();

        // MATERIAL_PENDING -> modifiable
        ProductionOrder pendingOrder = ProductionOrder.create(
            new ProductionOrderId(UUID.randomUUID()),
            new ProductionOrderNumber("PO-SH-202602-00002"),
            UUID.randomUUID(), VIN_VALUE,
            partiallyAvailableBom(), TEMPLATES);
        assertThat(pendingOrder.isModifiable()).isTrue();

        // IN_PRODUCTION -> not modifiable
        ProductionOrder inProdOrder = createInProductionOrder();
        assertThat(inProdOrder.isModifiable()).isFalse();
    }

    /**
     * Helper: creates a production order in IN_PRODUCTION status.
     */
    private ProductionOrder createInProductionOrder() {
        ProductionOrder order = ProductionOrder.create(
            ORDER_ID, ORDER_NUMBER, SOURCE_ORDER_ID, VIN_VALUE,
            allAvailableBom(), TEMPLATES);
        order.startProduction("OP-001", "WS-BODY");
        return order;
    }
}
