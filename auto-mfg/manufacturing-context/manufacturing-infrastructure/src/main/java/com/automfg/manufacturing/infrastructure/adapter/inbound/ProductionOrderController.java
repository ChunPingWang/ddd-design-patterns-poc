package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.CompleteAssemblyStepUseCase;
import com.automfg.manufacturing.application.usecase.StartProductionUseCase;
import com.automfg.manufacturing.infrastructure.persistence.AssemblyProcessJpaEntity;
import com.automfg.manufacturing.infrastructure.persistence.AssemblyStepJpaEntity;
import com.automfg.manufacturing.infrastructure.persistence.ProductionOrderJpaEntity;
import com.automfg.manufacturing.infrastructure.persistence.ProductionOrderJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production-orders")
public class ProductionOrderController {

    private final ProductionOrderJpaRepository jpaRepository;
    private final StartProductionUseCase startProductionUseCase;
    private final CompleteAssemblyStepUseCase completeAssemblyStepUseCase;

    public ProductionOrderController(ProductionOrderJpaRepository jpaRepository,
                                     StartProductionUseCase startProductionUseCase,
                                     CompleteAssemblyStepUseCase completeAssemblyStepUseCase) {
        this.jpaRepository = jpaRepository;
        this.startProductionUseCase = startProductionUseCase;
        this.completeAssemblyStepUseCase = completeAssemblyStepUseCase;
    }

    // --- DTOs ---

    record ProductionOrderSummaryDto(
        UUID id,
        String orderNumber,
        String vin,
        String status,
        Integer currentStationSequence,
        LocalDateTime createdAt
    ) {}

    record ProductionOrderDetailDto(
        UUID id,
        String orderNumber,
        UUID sourceOrderId,
        String vin,
        String status,
        Integer currentStationSequence,
        LocalDateTime scheduledStartDate,
        LocalDateTime createdAt,
        String assemblyProcessStatus,
        int totalAssemblySteps
    ) {}

    record StartProductionRequest(String operatorId, String workstationCode) {}

    record StartProductionResponse(UUID productionOrderId, String status) {}

    record CompleteAssemblyStepRequest(String operatorId, String materialBatchId, int actualMinutes) {}

    record CompleteAssemblyStepResponse(
        UUID productionOrderId,
        UUID assemblyStepId,
        String orderStatus,
        boolean overtimeAlert,
        boolean stationCompleted,
        boolean assemblyCompleted
    ) {}

    record AssemblyStepDto(
        UUID id,
        String workStationCode,
        int workStationSequence,
        String taskDescription,
        int standardTimeMinutes,
        String status,
        String operatorId,
        String materialBatchId,
        Integer actualTimeMinutes,
        LocalDateTime completedAt
    ) {}

    // --- Endpoints ---

    @GetMapping
    public ResponseEntity<List<ProductionOrderSummaryDto>> listProductionOrders(
            @RequestParam(required = false) String status) {
        List<ProductionOrderJpaEntity> entities;
        if (status != null && !status.isBlank()) {
            entities = jpaRepository.findByStatus(status);
        } else {
            entities = jpaRepository.findAll();
        }

        List<ProductionOrderSummaryDto> result = entities.stream()
            .map(e -> new ProductionOrderSummaryDto(
                e.getId(), e.getOrderNumber(), e.getVin(),
                e.getStatus(), e.getCurrentStationSequence(), e.getCreatedAt()))
            .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductionOrderDetailDto> getProductionOrder(@PathVariable UUID id) {
        return jpaRepository.findById(id)
            .map(e -> {
                AssemblyProcessJpaEntity ap = e.getAssemblyProcess();
                String apStatus = ap != null ? ap.getStatus() : null;
                int totalSteps = ap != null ? ap.getSteps().size() : 0;

                return new ProductionOrderDetailDto(
                    e.getId(), e.getOrderNumber(), e.getSourceOrderId(),
                    e.getVin(), e.getStatus(), e.getCurrentStationSequence(),
                    e.getScheduledStartDate(), e.getCreatedAt(),
                    apStatus, totalSteps);
            })
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<StartProductionResponse> startProduction(
            @PathVariable UUID id,
            @RequestBody StartProductionRequest request) {
        StartProductionUseCase.StartProductionResult result = startProductionUseCase.execute(
            new StartProductionUseCase.StartProductionCommand(
                id, request.operatorId(), request.workstationCode()));

        return ResponseEntity.ok(new StartProductionResponse(
            result.productionOrderId(), result.status()));
    }

    @PostMapping("/{id}/assembly-steps/{stepId}/complete")
    public ResponseEntity<CompleteAssemblyStepResponse> completeAssemblyStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId,
            @RequestBody CompleteAssemblyStepRequest request) {
        CompleteAssemblyStepUseCase.CompleteAssemblyStepResult result =
            completeAssemblyStepUseCase.execute(
                new CompleteAssemblyStepUseCase.CompleteAssemblyStepCommand(
                    id, stepId, request.operatorId(),
                    request.materialBatchId(), request.actualMinutes()));

        return ResponseEntity.ok(new CompleteAssemblyStepResponse(
            result.productionOrderId(), result.assemblyStepId(),
            result.orderStatus(), result.overtimeAlert(),
            result.stationCompleted(), result.assemblyCompleted()));
    }

    @GetMapping("/{id}/assembly-steps")
    public ResponseEntity<List<AssemblyStepDto>> getAssemblySteps(
            @PathVariable UUID id,
            @RequestParam(required = false) String stationCode) {
        return jpaRepository.findById(id)
            .map(e -> {
                AssemblyProcessJpaEntity ap = e.getAssemblyProcess();
                if (ap == null) {
                    return ResponseEntity.ok(List.<AssemblyStepDto>of());
                }

                List<AssemblyStepJpaEntity> steps = ap.getSteps();
                if (stationCode != null && !stationCode.isBlank()) {
                    steps = steps.stream()
                        .filter(s -> s.getWorkStationCode().equals(stationCode))
                        .toList();
                }

                List<AssemblyStepDto> dtos = steps.stream()
                    .map(s -> new AssemblyStepDto(
                        s.getId(), s.getWorkStationCode(), s.getWorkStationSequence(),
                        s.getTaskDescription(), s.getStandardTimeMinutes(),
                        s.getStatus(), s.getOperatorId(), s.getMaterialBatchId(),
                        s.getActualTimeMinutes(), s.getCompletedAt()))
                    .toList();

                return ResponseEntity.ok(dtos);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
