package com.automfg.manufacturing.infrastructure.adapter.inbound;

import com.automfg.manufacturing.application.usecase.CompleteAssemblyStepUseCase;
import com.automfg.manufacturing.application.usecase.GetAssemblyStepsUseCase;
import com.automfg.manufacturing.application.usecase.GetProductionOrderUseCase;
import com.automfg.manufacturing.application.usecase.ListProductionOrdersUseCase;
import com.automfg.manufacturing.application.usecase.StartProductionUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/production-orders")
public class ProductionOrderController {

    // Command use cases
    private final StartProductionUseCase startProductionUseCase;
    private final CompleteAssemblyStepUseCase completeAssemblyStepUseCase;

    // Query use cases (CQRS read path)
    private final GetProductionOrderUseCase getProductionOrderUseCase;
    private final ListProductionOrdersUseCase listProductionOrdersUseCase;
    private final GetAssemblyStepsUseCase getAssemblyStepsUseCase;

    public ProductionOrderController(StartProductionUseCase startProductionUseCase,
                                     CompleteAssemblyStepUseCase completeAssemblyStepUseCase,
                                     GetProductionOrderUseCase getProductionOrderUseCase,
                                     ListProductionOrdersUseCase listProductionOrdersUseCase,
                                     GetAssemblyStepsUseCase getAssemblyStepsUseCase) {
        this.startProductionUseCase = startProductionUseCase;
        this.completeAssemblyStepUseCase = completeAssemblyStepUseCase;
        this.getProductionOrderUseCase = getProductionOrderUseCase;
        this.listProductionOrdersUseCase = listProductionOrdersUseCase;
        this.getAssemblyStepsUseCase = getAssemblyStepsUseCase;
    }

    // --- Command DTOs ---

    record StartProductionRequest(String operatorId, String workstationCode) {}
    record StartProductionResponse(UUID productionOrderId, String status) {}

    record CompleteAssemblyStepRequest(String operatorId, String materialBatchId, int actualMinutes) {}
    record CompleteAssemblyStepResponse(
        UUID productionOrderId, UUID assemblyStepId, String orderStatus,
        boolean overtimeAlert, boolean stationCompleted, boolean assemblyCompleted
    ) {}

    // --- Command Endpoints ---

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

    // --- Query Endpoints (CQRS read path) ---

    @GetMapping
    public ResponseEntity<List<ListProductionOrdersUseCase.ProductionOrderSummary>> listProductionOrders(
            @RequestParam(required = false) String status) {
        List<ListProductionOrdersUseCase.ProductionOrderSummary> result =
            listProductionOrdersUseCase.execute(
                new ListProductionOrdersUseCase.ListProductionOrdersQuery(status));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductionOrder(@PathVariable UUID id) {
        try {
            GetProductionOrderUseCase.ProductionOrderDetail result =
                getProductionOrderUseCase.execute(
                    new GetProductionOrderUseCase.GetProductionOrderQuery(id));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/assembly-steps")
    public ResponseEntity<List<GetAssemblyStepsUseCase.AssemblyStepDetail>> getAssemblySteps(
            @PathVariable UUID id,
            @RequestParam(required = false) String stationCode) {
        List<GetAssemblyStepsUseCase.AssemblyStepDetail> result =
            getAssemblyStepsUseCase.execute(
                new GetAssemblyStepsUseCase.GetAssemblyStepsQuery(id, stationCode));
        return ResponseEntity.ok(result);
    }
}
