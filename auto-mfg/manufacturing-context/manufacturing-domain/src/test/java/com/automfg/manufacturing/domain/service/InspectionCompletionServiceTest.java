package com.automfg.manufacturing.domain.service;

import com.automfg.manufacturing.domain.model.*;
import com.automfg.manufacturing.domain.port.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionCompletionServiceTest {

    private ProductionOrder savedProductionOrder;
    private ProductionOrderRepository productionOrderRepository;
    private InspectionCompletionService service;

    private ProductionOrderId productionOrderId;
    private QualityInspectionId inspectionId;
    private VIN vin;

    @BeforeEach
    void setUp() {
        savedProductionOrder = null;

        productionOrderRepository = new ProductionOrderRepository() {
            @Override
            public ProductionOrder save(ProductionOrder order) {
                savedProductionOrder = order;
                return order;
            }

            @Override
            public Optional<ProductionOrder> findById(ProductionOrderId id) {
                return Optional.empty();
            }

            @Override
            public boolean existsBySourceOrderId(UUID sourceOrderId) {
                return false;
            }
        };

        service = new InspectionCompletionService(productionOrderRepository);

        productionOrderId = new ProductionOrderId(UUID.randomUUID());
        inspectionId = new QualityInspectionId(UUID.randomUUID());
        vin = new VIN("1HGBH41JXMN109186");
    }

    private QualityInspection createCompletedInspection(InspectionResult expectedResult) {
        List<ChecklistItemTemplate> checklist = List.of(
            new ChecklistItemTemplate("Brake System Inspection", true),
            new ChecklistItemTemplate("Paint Quality Check", false)
        );

        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, "inspector-001", checklist);

        if (expectedResult == InspectionResult.FAILED) {
            // Fail a safety item
            inspection.recordItemResult(
                inspection.getItems().get(0).getId(), InspectionItemStatus.FAILED, "Brake failure");
            inspection.recordItemResult(
                inspection.getItems().get(1).getId(), InspectionItemStatus.PASSED, null);
        } else {
            // Pass all items
            inspection.recordItemResult(
                inspection.getItems().get(0).getId(), InspectionItemStatus.PASSED, null);
            inspection.recordItemResult(
                inspection.getItems().get(1).getId(), InspectionItemStatus.PASSED, null);
        }

        inspection.complete("inspector-001");
        return inspection;
    }

    private ProductionOrder createProductionOrderWithStatus(ProductionOrderStatus status) {
        return ProductionOrder.reconstitute(
            productionOrderId,
            new ProductionOrderNumber("PO-SH-202601-00001"),
            UUID.randomUUID(),
            vin,
            status,
            null, null, null, null,
            LocalDateTime.now()
        );
    }

    @Test
    void complete_review_passed_marks_production_order_inspection_passed() {
        QualityInspection inspection = createCompletedInspection(InspectionResult.PASSED);
        ProductionOrder productionOrder = createProductionOrderWithStatus(ProductionOrderStatus.ASSEMBLY_COMPLETED);

        service.completeInspectionReview(inspection, productionOrder);

        assertThat(productionOrder.getStatus()).isEqualTo(ProductionOrderStatus.INSPECTION_PASSED);
        assertThat(savedProductionOrder).isSameAs(productionOrder);
    }

    @Test
    void complete_review_failed_marks_production_order_inspection_failed() {
        QualityInspection inspection = createCompletedInspection(InspectionResult.FAILED);
        ProductionOrder productionOrder = createProductionOrderWithStatus(ProductionOrderStatus.ASSEMBLY_COMPLETED);

        service.completeInspectionReview(inspection, productionOrder);

        assertThat(productionOrder.getStatus()).isEqualTo(ProductionOrderStatus.INSPECTION_FAILED);
        assertThat(savedProductionOrder).isSameAs(productionOrder);
    }
}
