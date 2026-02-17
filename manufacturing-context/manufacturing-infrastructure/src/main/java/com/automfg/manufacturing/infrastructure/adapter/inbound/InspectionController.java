package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionController {

    // Command use cases
    private final CreateInspectionUseCase createInspectionUseCase;
    private final RecordInspectionItemResultUseCase recordInspectionItemResultUseCase;
    private final CompleteInspectionUseCase completeInspectionUseCase;
    private final ReviewInspectionUseCase reviewInspectionUseCase;

    // Query use cases (CQRS read path)
    private final GetInspectionUseCase getInspectionUseCase;

    public InspectionController(CreateInspectionUseCase createInspectionUseCase,
                                 RecordInspectionItemResultUseCase recordInspectionItemResultUseCase,
                                 CompleteInspectionUseCase completeInspectionUseCase,
                                 ReviewInspectionUseCase reviewInspectionUseCase,
                                 GetInspectionUseCase getInspectionUseCase) {
        this.createInspectionUseCase = createInspectionUseCase;
        this.recordInspectionItemResultUseCase = recordInspectionItemResultUseCase;
        this.completeInspectionUseCase = completeInspectionUseCase;
        this.reviewInspectionUseCase = reviewInspectionUseCase;
        this.getInspectionUseCase = getInspectionUseCase;
    }

    // --- Command Endpoints ---

    @PostMapping
    public ResponseEntity<CreateInspectionResponse> createInspection(
            @RequestBody CreateInspectionRequest request) {
        var command = new CreateInspectionUseCase.CreateInspectionCommand(
            request.productionOrderId(), request.vehicleModelCode(), request.inspectorId());
        var result = createInspectionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateInspectionResponse(result.inspectionId(), result.itemCount()));
    }

    @PostMapping("/{inspectionId}/items/{itemId}/result")
    public ResponseEntity<Void> recordItemResult(
            @PathVariable UUID inspectionId,
            @PathVariable UUID itemId,
            @RequestBody RecordItemResultRequest request) {
        var command = new RecordInspectionItemResultUseCase.RecordItemResultCommand(
            inspectionId, itemId, request.status(), request.notes());
        recordInspectionItemResultUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{inspectionId}/complete")
    public ResponseEntity<CompleteInspectionResponse> completeInspection(
            @PathVariable UUID inspectionId,
            @RequestBody CompleteInspectionRequest request) {
        var command = new CompleteInspectionUseCase.CompleteInspectionCommand(
            inspectionId, request.inspectorId());
        var result = completeInspectionUseCase.execute(command);
        return ResponseEntity.ok(new CompleteInspectionResponse(result.inspectionId(), result.result()));
    }

    @PostMapping("/{inspectionId}/review")
    public ResponseEntity<ReviewInspectionResponse> reviewInspection(
            @PathVariable UUID inspectionId,
            @RequestBody ReviewInspectionRequest request) {
        var command = new ReviewInspectionUseCase.ReviewInspectionCommand(
            inspectionId, request.reviewerId());
        var result = reviewInspectionUseCase.execute(command);
        return ResponseEntity.ok(new ReviewInspectionResponse(
            result.inspectionId(), result.result(), result.reviewerId()));
    }

    // --- Query Endpoints (CQRS read path) ---

    @GetMapping("/{inspectionId}")
    public ResponseEntity<?> getInspection(@PathVariable UUID inspectionId) {
        try {
            GetInspectionUseCase.InspectionDetail result = getInspectionUseCase.execute(
                new GetInspectionUseCase.GetInspectionQuery(inspectionId));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Request/Response DTOs ---

    record CreateInspectionRequest(UUID productionOrderId, String vehicleModelCode, String inspectorId) {}
    record CreateInspectionResponse(UUID inspectionId, int itemCount) {}

    record RecordItemResultRequest(String status, String notes) {}

    record CompleteInspectionRequest(String inspectorId) {}
    record CompleteInspectionResponse(UUID inspectionId, String result) {}

    record ReviewInspectionRequest(String reviewerId) {}
    record ReviewInspectionResponse(UUID inspectionId, String result, String reviewerId) {}
}
