package com.automfg.manufacturing.domain.model;

import com.automfg.manufacturing.domain.event.InspectionCompletedEvent;
import com.automfg.manufacturing.domain.event.InspectionCreatedEvent;
import com.automfg.manufacturing.domain.event.InspectionFailedEvent;
import com.automfg.manufacturing.domain.event.InspectionReviewedEvent;
import com.automfg.manufacturing.domain.event.VehicleCompletedEvent;
import com.automfg.shared.domain.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QualityInspectionTest {

    private QualityInspectionId inspectionId;
    private ProductionOrderId productionOrderId;
    private VIN vin;
    private String inspectorId;
    private String reviewerId;

    @BeforeEach
    void setUp() {
        inspectionId = new QualityInspectionId(UUID.randomUUID());
        productionOrderId = new ProductionOrderId(UUID.randomUUID());
        vin = new VIN("1HGBH41JXMN109186");
        inspectorId = "inspector-001";
        reviewerId = "reviewer-002";
    }

    // --- Helper methods ---

    private List<ChecklistItemTemplate> standardChecklist() {
        return List.of(
            new ChecklistItemTemplate("Brake System Inspection", true),
            new ChecklistItemTemplate("Steering System Check", true),
            new ChecklistItemTemplate("Paint Quality Check", false),
            new ChecklistItemTemplate("Interior Trim Alignment", false)
        );
    }

    private List<ChecklistItemTemplate> checklistWithManySafetyItems() {
        return List.of(
            new ChecklistItemTemplate("Brake System Inspection", true),
            new ChecklistItemTemplate("Steering System Check", true),
            new ChecklistItemTemplate("Airbag System Verification", true),
            new ChecklistItemTemplate("Paint Quality Check", false),
            new ChecklistItemTemplate("Interior Trim Alignment", false),
            new ChecklistItemTemplate("Infotainment System Test", false),
            new ChecklistItemTemplate("Exterior Light Function", false),
            new ChecklistItemTemplate("Windshield Wiper Check", false)
        );
    }

    private void recordAllItemsAs(QualityInspection inspection, InspectionItemStatus status) {
        for (InspectionItem item : inspection.getItems()) {
            inspection.recordItemResult(item.getId(), status, null);
        }
    }

    // --- Test 1: Create inspection with checklist ---

    @Test
    void create_inspection_with_checklist() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        assertThat(inspection.getId()).isEqualTo(inspectionId);
        assertThat(inspection.getProductionOrderId()).isEqualTo(productionOrderId);
        assertThat(inspection.getVin()).isEqualTo(vin);
        assertThat(inspection.getInspectorId()).isEqualTo(inspectorId);
        assertThat(inspection.getResult()).isNull();
        assertThat(inspection.getReviewerId()).isNull();
        assertThat(inspection.getInspectedAt()).isNull();
        assertThat(inspection.getReviewedAt()).isNull();
        assertThat(inspection.getCreatedAt()).isNotNull();
        assertThat(inspection.getCorrectsRecordId()).isNull();

        // All items should be PENDING
        assertThat(inspection.getItems()).hasSize(4);
        assertThat(inspection.getItems()).allMatch(InspectionItem::isPending);

        // Verify safety flags are correct
        assertThat(inspection.getItems().get(0).isSafetyRelated()).isTrue();
        assertThat(inspection.getItems().get(1).isSafetyRelated()).isTrue();
        assertThat(inspection.getItems().get(2).isSafetyRelated()).isFalse();
        assertThat(inspection.getItems().get(3).isSafetyRelated()).isFalse();

        // Should have registered InspectionCreatedEvent
        assertThat(inspection.getDomainEvents()).hasSize(1);
        assertThat(inspection.getDomainEvents().get(0)).isInstanceOf(InspectionCreatedEvent.class);
    }

    // --- Test 2: Record item result success ---

    @Test
    void record_item_result_success() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        InspectionItem firstItem = inspection.getItems().get(0);
        inspection.recordItemResult(firstItem.getId(), InspectionItemStatus.PASSED, "Looks good");

        assertThat(firstItem.getStatus()).isEqualTo(InspectionItemStatus.PASSED);
        assertThat(firstItem.getNotes()).isEqualTo("Looks good");
        assertThat(firstItem.isPending()).isFalse();
    }

    // --- Test 3: Record already recorded item throws ---

    @Test
    void record_already_recorded_item_throws() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        InspectionItem firstItem = inspection.getItems().get(0);
        inspection.recordItemResult(firstItem.getId(), InspectionItemStatus.PASSED, null);

        assertThatThrownBy(() ->
            inspection.recordItemResult(firstItem.getId(), InspectionItemStatus.FAILED, "Retry"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already has status");
    }

    // --- Test 4: Complete all passed -> result = PASSED ---

    @Test
    void complete_all_passed() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        recordAllItemsAs(inspection, InspectionItemStatus.PASSED);
        inspection.complete(inspectorId);

        assertThat(inspection.getResult()).isEqualTo(InspectionResult.PASSED);
        assertThat(inspection.getInspectedAt()).isNotNull();

        // Should have InspectionCreatedEvent + InspectionCompletedEvent
        List<DomainEvent> events = inspection.getDomainEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(InspectionCompletedEvent.class);

        InspectionCompletedEvent completedEvent = (InspectionCompletedEvent) events.get(1);
        assertThat(completedEvent.getResult()).isEqualTo("PASSED");
    }

    // --- Test 5: Complete safety item failed -> result = FAILED (BR-10) ---

    @Test
    void complete_safety_item_failed() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        // First item is safety-related, mark it FAILED
        inspection.recordItemResult(inspection.getItems().get(0).getId(), InspectionItemStatus.FAILED, "Brake failure");
        // Rest pass
        inspection.recordItemResult(inspection.getItems().get(1).getId(), InspectionItemStatus.PASSED, null);
        inspection.recordItemResult(inspection.getItems().get(2).getId(), InspectionItemStatus.PASSED, null);
        inspection.recordItemResult(inspection.getItems().get(3).getId(), InspectionItemStatus.PASSED, null);

        inspection.complete(inspectorId);

        assertThat(inspection.getResult()).isEqualTo(InspectionResult.FAILED);
    }

    // --- Test 6: Complete three conditional non-safety -> result = CONDITIONAL_PASS (BR-11) ---

    @Test
    void complete_three_conditional_non_safety() {
        // Need at least 3 non-safety items that can be CONDITIONAL
        List<ChecklistItemTemplate> checklist = List.of(
            new ChecklistItemTemplate("Brake System Inspection", true),
            new ChecklistItemTemplate("Paint Quality Check", false),
            new ChecklistItemTemplate("Interior Trim Alignment", false),
            new ChecklistItemTemplate("Infotainment System Test", false)
        );

        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, checklist);

        // Safety item passes
        inspection.recordItemResult(inspection.getItems().get(0).getId(), InspectionItemStatus.PASSED, null);
        // 3 non-safety items are CONDITIONAL
        inspection.recordItemResult(inspection.getItems().get(1).getId(), InspectionItemStatus.CONDITIONAL, "Minor scratch");
        inspection.recordItemResult(inspection.getItems().get(2).getId(), InspectionItemStatus.CONDITIONAL, "Slight gap");
        inspection.recordItemResult(inspection.getItems().get(3).getId(), InspectionItemStatus.CONDITIONAL, "Display lag");

        inspection.complete(inspectorId);

        assertThat(inspection.getResult()).isEqualTo(InspectionResult.CONDITIONAL_PASS);
    }

    // --- Test 7: Complete four conditional non-safety -> result = FAILED (BR-11, exceeds limit) ---

    @Test
    void complete_four_conditional_non_safety() {
        List<ChecklistItemTemplate> checklist = List.of(
            new ChecklistItemTemplate("Brake System Inspection", true),
            new ChecklistItemTemplate("Paint Quality Check", false),
            new ChecklistItemTemplate("Interior Trim Alignment", false),
            new ChecklistItemTemplate("Infotainment System Test", false),
            new ChecklistItemTemplate("Exterior Light Function", false)
        );

        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, checklist);

        // Safety item passes
        inspection.recordItemResult(inspection.getItems().get(0).getId(), InspectionItemStatus.PASSED, null);
        // 4 non-safety items are CONDITIONAL -> exceeds limit of 3
        inspection.recordItemResult(inspection.getItems().get(1).getId(), InspectionItemStatus.CONDITIONAL, null);
        inspection.recordItemResult(inspection.getItems().get(2).getId(), InspectionItemStatus.CONDITIONAL, null);
        inspection.recordItemResult(inspection.getItems().get(3).getId(), InspectionItemStatus.CONDITIONAL, null);
        inspection.recordItemResult(inspection.getItems().get(4).getId(), InspectionItemStatus.CONDITIONAL, null);

        inspection.complete(inspectorId);

        assertThat(inspection.getResult()).isEqualTo(InspectionResult.FAILED);
    }

    // --- Test 8: Complete with pending items throws ---

    @Test
    void complete_with_pending_items_throws() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        // Record only first 2 items, leave 2 pending
        inspection.recordItemResult(inspection.getItems().get(0).getId(), InspectionItemStatus.PASSED, null);
        inspection.recordItemResult(inspection.getItems().get(1).getId(), InspectionItemStatus.PASSED, null);

        assertThatThrownBy(() -> inspection.complete(inspectorId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not all items have been recorded");
    }

    // --- Test 9: Review four-eyes success ---

    @Test
    void review_four_eyes_success() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        recordAllItemsAs(inspection, InspectionItemStatus.PASSED);
        inspection.complete(inspectorId);
        inspection.clearDomainEvents();

        inspection.review(reviewerId);

        assertThat(inspection.getReviewerId()).isEqualTo(reviewerId);
        assertThat(inspection.getReviewedAt()).isNotNull();
    }

    // --- Test 10: Review same inspector throws (BR-12) ---

    @Test
    void review_same_inspector_throws() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        recordAllItemsAs(inspection, InspectionItemStatus.PASSED);
        inspection.complete(inspectorId);

        assertThatThrownBy(() -> inspection.review(inspectorId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("four-eyes");
    }

    // --- Test 11: Review before completion throws ---

    @Test
    void review_before_completion_throws() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        assertThatThrownBy(() -> inspection.review(reviewerId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not been completed");
    }

    // --- Test 12: Review passed registers VehicleCompletedEvent ---

    @Test
    void review_passed_registers_vehicle_completed_event() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        recordAllItemsAs(inspection, InspectionItemStatus.PASSED);
        inspection.complete(inspectorId);
        inspection.clearDomainEvents();

        inspection.review(reviewerId);

        List<DomainEvent> events = inspection.getDomainEvents();

        // Should have VehicleCompletedEvent and InspectionReviewedEvent
        assertThat(events).hasSize(2);

        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(VehicleCompletedEvent.class);
            VehicleCompletedEvent vehicleEvent = (VehicleCompletedEvent) event;
            assertThat(vehicleEvent.getProductionOrderId()).isEqualTo(productionOrderId.value());
            assertThat(vehicleEvent.getVin()).isEqualTo(vin.value());
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(InspectionReviewedEvent.class);
            InspectionReviewedEvent reviewedEvent = (InspectionReviewedEvent) event;
            assertThat(reviewedEvent.getReviewerId()).isEqualTo(reviewerId);
            assertThat(reviewedEvent.getResult()).isEqualTo("PASSED");
        });
    }

    // --- Additional: Review failed registers InspectionFailedEvent ---

    @Test
    void review_failed_registers_inspection_failed_event() {
        QualityInspection inspection = QualityInspection.create(
            inspectionId, productionOrderId, vin, inspectorId, standardChecklist());

        // Fail a safety item to trigger FAILED result
        inspection.recordItemResult(inspection.getItems().get(0).getId(), InspectionItemStatus.FAILED, "Brake issue");
        inspection.recordItemResult(inspection.getItems().get(1).getId(), InspectionItemStatus.PASSED, null);
        inspection.recordItemResult(inspection.getItems().get(2).getId(), InspectionItemStatus.PASSED, null);
        inspection.recordItemResult(inspection.getItems().get(3).getId(), InspectionItemStatus.PASSED, null);

        inspection.complete(inspectorId);
        inspection.clearDomainEvents();

        inspection.review(reviewerId);

        List<DomainEvent> events = inspection.getDomainEvents();

        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(InspectionFailedEvent.class);
            InspectionFailedEvent failedEvent = (InspectionFailedEvent) event;
            assertThat(failedEvent.getFailedItemDescriptions()).contains("Brake System Inspection");
        });

        assertThat(events).anySatisfy(event ->
            assertThat(event).isInstanceOf(InspectionReviewedEvent.class));

        // Should NOT have VehicleCompletedEvent
        assertThat(events).noneMatch(event -> event instanceof VehicleCompletedEvent);
    }
}
