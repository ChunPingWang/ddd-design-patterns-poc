package com.automfg.manufacturing.domain.model;

import com.automfg.manufacturing.domain.event.InspectionCompletedEvent;
import com.automfg.manufacturing.domain.event.InspectionCreatedEvent;
import com.automfg.manufacturing.domain.event.InspectionFailedEvent;
import com.automfg.manufacturing.domain.event.InspectionReviewedEvent;
import com.automfg.manufacturing.domain.event.VehicleCompletedEvent;
import com.automfg.shared.domain.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for quality inspections in the manufacturing domain.
 * Enforces business rules:
 * - BR-10: Any safety-related item FAILED -> entire inspection FAILED
 * - BR-11: Max 3 non-safety CONDITIONAL items for CONDITIONAL_PASS, more -> FAILED
 * - BR-12: Four-eyes principle — reviewer must differ from inspector
 */
public class QualityInspection extends AggregateRoot {

    private static final int MAX_CONDITIONAL_NON_SAFETY_ITEMS = 3;

    private final QualityInspectionId id;
    private final ProductionOrderId productionOrderId;
    private final VIN vin;
    private InspectionResult result;
    private String inspectorId;
    private String reviewerId;
    private LocalDateTime inspectedAt;
    private LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;
    private final List<InspectionItem> items;
    private final UUID correctsRecordId;

    private QualityInspection(QualityInspectionId id, ProductionOrderId productionOrderId,
                               VIN vin, String inspectorId, List<InspectionItem> items,
                               UUID correctsRecordId, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "QualityInspectionId must not be null");
        this.productionOrderId = Objects.requireNonNull(productionOrderId, "ProductionOrderId must not be null");
        this.vin = Objects.requireNonNull(vin, "VIN must not be null");
        this.inspectorId = Objects.requireNonNull(inspectorId, "InspectorId must not be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items must not be null"));
        this.correctsRecordId = correctsRecordId;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        this.result = null;
        this.reviewerId = null;
        this.inspectedAt = null;
        this.reviewedAt = null;
    }

    /**
     * Factory method: Creates a new QualityInspection from checklist item templates.
     * All items start as PENDING. Registers InspectionCreatedEvent.
     */
    public static QualityInspection create(QualityInspectionId id, ProductionOrderId productionOrderId,
                                            VIN vin, String inspectorId,
                                            List<ChecklistItemTemplate> checklistItems) {
        Objects.requireNonNull(checklistItems, "Checklist items must not be null");
        if (checklistItems.isEmpty()) {
            throw new IllegalArgumentException("Checklist items must not be empty");
        }

        List<InspectionItem> items = checklistItems.stream()
            .map(template -> new InspectionItem(
                new InspectionItemId(UUID.randomUUID()),
                template.description(),
                template.safetyRelated()))
            .toList();

        QualityInspection inspection = new QualityInspection(
            id, productionOrderId, vin, inspectorId, items, null, LocalDateTime.now());

        inspection.registerEvent(new InspectionCreatedEvent(
            id.value(), productionOrderId.value(), vin.value(), inspectorId));

        return inspection;
    }

    /**
     * Factory method: Creates a new QualityInspection for re-inspection after rework.
     * Sets correctsRecordId to link to the previous failed inspection.
     */
    public static QualityInspection createForReinspection(QualityInspectionId id,
                                                            ProductionOrderId productionOrderId,
                                                            VIN vin, String inspectorId,
                                                            List<ChecklistItemTemplate> checklistItems,
                                                            UUID previousInspectionId) {
        Objects.requireNonNull(previousInspectionId, "Previous inspection ID must not be null");
        Objects.requireNonNull(checklistItems, "Checklist items must not be null");
        if (checklistItems.isEmpty()) {
            throw new IllegalArgumentException("Checklist items must not be empty");
        }

        List<InspectionItem> items = checklistItems.stream()
            .map(template -> new InspectionItem(
                new InspectionItemId(UUID.randomUUID()),
                template.description(),
                template.safetyRelated()))
            .toList();

        QualityInspection inspection = new QualityInspection(
            id, productionOrderId, vin, inspectorId, items, previousInspectionId, LocalDateTime.now());

        inspection.registerEvent(new InspectionCreatedEvent(
            id.value(), productionOrderId.value(), vin.value(), inspectorId));

        return inspection;
    }

    /**
     * Reconstitutes a QualityInspection from persistence — no events registered.
     */
    public static QualityInspection reconstitute(QualityInspectionId id, ProductionOrderId productionOrderId,
                                                  VIN vin, InspectionResult result, String inspectorId,
                                                  String reviewerId, LocalDateTime inspectedAt,
                                                  LocalDateTime reviewedAt, LocalDateTime createdAt,
                                                  List<InspectionItem> items, UUID correctsRecordId) {
        QualityInspection inspection = new QualityInspection(
            id, productionOrderId, vin, inspectorId, items, correctsRecordId, createdAt);
        inspection.result = result;
        inspection.reviewerId = reviewerId;
        inspection.inspectedAt = inspectedAt;
        inspection.reviewedAt = reviewedAt;
        return inspection;
    }

    /**
     * Records the result for a specific inspection item.
     * The item must be in PENDING status.
     */
    public void recordItemResult(InspectionItemId itemId, InspectionItemStatus status, String notes) {
        Objects.requireNonNull(itemId, "InspectionItemId must not be null");

        InspectionItem item = items.stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Inspection item not found: " + itemId.value()));

        item.recordResult(status, notes);
    }

    /**
     * Completes the inspection by evaluating all items and determining the overall result.
     * Business rules:
     * - BR-10: Any safety-related item FAILED -> result = FAILED
     * - BR-11: Max 3 non-safety CONDITIONAL items -> CONDITIONAL_PASS, more -> FAILED
     * - Otherwise -> PASSED
     *
     * @param inspectorId must match the assigned inspector
     */
    public void complete(String inspectorId) {
        Objects.requireNonNull(inspectorId, "InspectorId must not be null");

        // Validate all items have been recorded
        boolean hasPendingItems = items.stream().anyMatch(InspectionItem::isPending);
        if (hasPendingItems) {
            throw new IllegalStateException(
                "Cannot complete inspection: not all items have been recorded");
        }

        // Validate inspector identity
        if (!this.inspectorId.equals(inspectorId)) {
            throw new IllegalArgumentException(
                "Inspector ID does not match the assigned inspector");
        }

        // Evaluate result
        this.result = evaluateResult();
        this.inspectedAt = LocalDateTime.now();

        registerEvent(new InspectionCompletedEvent(
            id.value(), productionOrderId.value(), result.name()));
    }

    /**
     * Reviews the completed inspection. Enforces the four-eyes principle (BR-12).
     *
     * @param reviewerId must differ from inspectorId
     */
    public void review(String reviewerId) {
        Objects.requireNonNull(reviewerId, "ReviewerId must not be null");

        // Must be completed first
        if (this.result == null) {
            throw new IllegalStateException(
                "Cannot review inspection: inspection has not been completed yet");
        }

        // BR-12: Four-eyes principle
        if (this.inspectorId.equals(reviewerId)) {
            throw new IllegalArgumentException(
                "BR-12 violation: reviewer must differ from inspector (four-eyes principle)");
        }

        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();

        // Register result-specific events
        if (this.result == InspectionResult.PASSED || this.result == InspectionResult.CONDITIONAL_PASS) {
            registerEvent(new VehicleCompletedEvent(productionOrderId.value(), vin.value()));
        }

        if (this.result == InspectionResult.FAILED) {
            List<String> failedDescriptions = items.stream()
                .filter(InspectionItem::isFailed)
                .map(InspectionItem::getDescription)
                .toList();
            registerEvent(new InspectionFailedEvent(
                id.value(), productionOrderId.value(), vin.value(), failedDescriptions));
        }

        registerEvent(new InspectionReviewedEvent(
            id.value(), productionOrderId.value(), result.name(), reviewerId));
    }

    private InspectionResult evaluateResult() {
        // BR-10: Any safety-related item FAILED -> FAILED
        boolean safetyFailure = items.stream()
            .anyMatch(item -> item.isSafetyRelated() && item.isFailed());
        if (safetyFailure) {
            return InspectionResult.FAILED;
        }

        // Count non-safety conditional items
        long conditionalNonSafetyCount = items.stream()
            .filter(item -> !item.isSafetyRelated() && item.isConditional())
            .count();

        // Any failed non-safety items also contribute to failure evaluation
        boolean anyNonSafetyFailed = items.stream()
            .anyMatch(item -> !item.isSafetyRelated() && item.isFailed());

        // Any safety-related conditional items count toward evaluation too
        long conditionalSafetyCount = items.stream()
            .filter(item -> item.isSafetyRelated() && item.isConditional())
            .count();

        long totalConditionalCount = conditionalNonSafetyCount + conditionalSafetyCount;

        if (anyNonSafetyFailed) {
            return InspectionResult.FAILED;
        }

        // BR-11: More than 3 conditional non-safety items -> FAILED
        if (conditionalNonSafetyCount > MAX_CONDITIONAL_NON_SAFETY_ITEMS) {
            return InspectionResult.FAILED;
        }

        // BR-11: Up to 3 conditional non-safety items -> CONDITIONAL_PASS
        if (totalConditionalCount > 0) {
            return InspectionResult.CONDITIONAL_PASS;
        }

        return InspectionResult.PASSED;
    }

    /**
     * Returns the descriptions of all failed items.
     */
    public List<String> getFailedItemDescriptions() {
        return items.stream()
            .filter(InspectionItem::isFailed)
            .map(InspectionItem::getDescription)
            .toList();
    }

    // Getters

    public QualityInspectionId getId() {
        return id;
    }

    public ProductionOrderId getProductionOrderId() {
        return productionOrderId;
    }

    public VIN getVin() {
        return vin;
    }

    public InspectionResult getResult() {
        return result;
    }

    public String getInspectorId() {
        return inspectorId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<InspectionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public UUID getCorrectsRecordId() {
        return correctsRecordId;
    }
}
