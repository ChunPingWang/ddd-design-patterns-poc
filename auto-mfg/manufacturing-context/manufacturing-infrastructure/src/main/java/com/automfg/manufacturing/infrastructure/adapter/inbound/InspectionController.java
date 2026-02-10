package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.*;
import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.model.QualityInspectionId;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionController {

    private final CreateInspectionUseCase createInspectionUseCase;
    private final RecordInspectionItemResultUseCase recordInspectionItemResultUseCase;
    private final CompleteInspectionUseCase completeInspectionUseCase;
    private final ReviewInspectionUseCase reviewInspectionUseCase;
    private final QualityInspectionRepository qualityInspectionRepository;

    public InspectionController(CreateInspectionUseCase createInspectionUseCase,
                                 RecordInspectionItemResultUseCase recordInspectionItemResultUseCase,
                                 CompleteInspectionUseCase completeInspectionUseCase,
                                 ReviewInspectionUseCase reviewInspectionUseCase,
                                 QualityInspectionRepository qualityInspectionRepository) {
        this.createInspectionUseCase = createInspectionUseCase;
        this.recordInspectionItemResultUseCase = recordInspectionItemResultUseCase;
        this.completeInspectionUseCase = completeInspectionUseCase;
        this.reviewInspectionUseCase = reviewInspectionUseCase;
        this.qualityInspectionRepository = qualityInspectionRepository;
    }

    @PostMapping
    public ResponseEntity<CreateInspectionResponse> createInspection(
            @RequestBody CreateInspectionRequest request) {
        var command = new CreateInspectionUseCase.CreateInspectionCommand(
            request.productionOrderId(), request.vehicleModelCode(), request.inspectorId());
        var result = createInspectionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateInspectionResponse(result.inspectionId(), result.itemCount()));
    }

    @GetMapping("/{inspectionId}")
    public ResponseEntity<InspectionDetailResponse> getInspection(
            @PathVariable UUID inspectionId) {
        return qualityInspectionRepository.findById(new QualityInspectionId(inspectionId))
            .map(inspection -> ResponseEntity.ok(toDetailResponse(inspection)))
            .orElse(ResponseEntity.notFound().build());
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

    // --- Request/Response DTOs ---

    record CreateInspectionRequest(UUID productionOrderId, String vehicleModelCode, String inspectorId) {}
    record CreateInspectionResponse(UUID inspectionId, int itemCount) {}

    record RecordItemResultRequest(String status, String notes) {}

    record CompleteInspectionRequest(String inspectorId) {}
    record CompleteInspectionResponse(UUID inspectionId, String result) {}

    record ReviewInspectionRequest(String reviewerId) {}
    record ReviewInspectionResponse(UUID inspectionId, String result, String reviewerId) {}

    record InspectionDetailResponse(
        UUID id,
        UUID productionOrderId,
        String vin,
        String result,
        String inspectorId,
        String reviewerId,
        int itemCount
    ) {}

    private InspectionDetailResponse toDetailResponse(QualityInspection inspection) {
        return new InspectionDetailResponse(
            inspection.getId().value(),
            inspection.getProductionOrderId().value(),
            inspection.getVin().value(),
            inspection.getResult() != null ? inspection.getResult().name() : null,
            inspection.getInspectorId(),
            inspection.getReviewerId(),
            inspection.getItems().size()
        );
    }
}
