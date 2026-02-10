package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.AssemblyProcess;
import com.automfg.manufacturing.domain.model.AssemblyProcessId;
import com.automfg.manufacturing.domain.model.AssemblyProcessStatus;
import com.automfg.manufacturing.domain.model.AssemblyStep;
import com.automfg.manufacturing.domain.model.AssemblyStepId;
import com.automfg.manufacturing.domain.model.AssemblyStepStatus;
import com.automfg.manufacturing.domain.model.BomLineItem;
import com.automfg.manufacturing.domain.model.BomSnapshot;
import com.automfg.manufacturing.domain.model.MaterialBatchId;
import com.automfg.manufacturing.domain.model.ProductionOrder;
import com.automfg.manufacturing.domain.model.ProductionOrderId;
import com.automfg.manufacturing.domain.model.ProductionOrderNumber;
import com.automfg.manufacturing.domain.model.ProductionOrderStatus;
import com.automfg.manufacturing.domain.model.VIN;
import com.automfg.manufacturing.domain.model.WorkStationId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductionOrderMapper {

    /**
     * Maps a domain ProductionOrder to its JPA entity graph.
     */
    public ProductionOrderJpaEntity toJpaEntity(ProductionOrder domain) {
        ProductionOrderJpaEntity entity = new ProductionOrderJpaEntity();
        entity.setId(domain.getId().value());
        entity.setOrderNumber(domain.getOrderNumber().value());
        entity.setSourceOrderId(domain.getSourceOrderId());
        entity.setVin(domain.getVin().value());
        entity.setStatus(domain.getStatus().name());
        entity.setCurrentStationSequence(domain.getCurrentStationSequence());
        entity.setScheduledStartDate(domain.getScheduledStartDate());
        entity.setCreatedAt(domain.getCreatedAt());

        // Map BOM snapshot
        if (domain.getBomSnapshot() != null) {
            BomSnapshotJpaEntity bomEntity = mapBomSnapshotToJpa(domain.getBomSnapshot(), entity);
            entity.setBomSnapshot(bomEntity);
        }

        // Map assembly process
        if (domain.getAssemblyProcess() != null) {
            AssemblyProcessJpaEntity processEntity = mapAssemblyProcessToJpa(domain.getAssemblyProcess(), entity);
            entity.setAssemblyProcess(processEntity);
        }

        return entity;
    }

    /**
     * Maps a JPA entity graph back to a domain ProductionOrder (reconstitute).
     */
    public ProductionOrder toDomain(ProductionOrderJpaEntity entity) {
        ProductionOrderId id = new ProductionOrderId(entity.getId());
        ProductionOrderNumber orderNumber = new ProductionOrderNumber(entity.getOrderNumber());
        VIN vin = new VIN(entity.getVin());
        ProductionOrderStatus status = ProductionOrderStatus.valueOf(entity.getStatus());

        // Map BOM snapshot
        BomSnapshot bomSnapshot = null;
        if (entity.getBomSnapshot() != null) {
            bomSnapshot = mapBomSnapshotToDomain(entity.getBomSnapshot());
        }

        // Map assembly process
        AssemblyProcess assemblyProcess = null;
        if (entity.getAssemblyProcess() != null) {
            assemblyProcess = mapAssemblyProcessToDomain(entity.getAssemblyProcess());
        }

        return ProductionOrder.reconstitute(
            id, orderNumber, entity.getSourceOrderId(), vin, status,
            bomSnapshot, assemblyProcess, entity.getCurrentStationSequence(),
            entity.getScheduledStartDate(), entity.getCreatedAt()
        );
    }

    // --- BOM Snapshot mapping ---

    private BomSnapshotJpaEntity mapBomSnapshotToJpa(BomSnapshot domain, ProductionOrderJpaEntity orderEntity) {
        BomSnapshotJpaEntity entity = new BomSnapshotJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductionOrder(orderEntity);
        entity.setSnapshotDate(domain.getSnapshotDate());

        List<BomLineItemJpaEntity> lineItemEntities = new ArrayList<>();
        for (BomLineItem item : domain.getLineItems()) {
            BomLineItemJpaEntity itemEntity = new BomLineItemJpaEntity();
            itemEntity.setId(UUID.randomUUID());
            itemEntity.setBomSnapshot(entity);
            itemEntity.setPartNumber(item.partNumber());
            itemEntity.setPartDescription(item.partDescription());
            itemEntity.setQuantityRequired(item.quantityRequired());
            itemEntity.setUnitOfMeasure(item.unitOfMeasure());
            itemEntity.setAvailable(item.available());
            lineItemEntities.add(itemEntity);
        }
        entity.setLineItems(lineItemEntities);

        return entity;
    }

    private BomSnapshot mapBomSnapshotToDomain(BomSnapshotJpaEntity entity) {
        List<BomLineItem> lineItems = entity.getLineItems().stream()
            .map(item -> new BomLineItem(
                item.getPartNumber(),
                item.getPartDescription(),
                item.getQuantityRequired(),
                item.getUnitOfMeasure(),
                item.isAvailable()
            ))
            .toList();

        return new BomSnapshot(lineItems);
    }

    // --- Assembly Process mapping ---

    private AssemblyProcessJpaEntity mapAssemblyProcessToJpa(AssemblyProcess domain,
                                                              ProductionOrderJpaEntity orderEntity) {
        AssemblyProcessJpaEntity entity = new AssemblyProcessJpaEntity();
        entity.setId(domain.getId().value());
        entity.setProductionOrder(orderEntity);
        entity.setStatus(domain.getStatus().name());

        List<AssemblyStepJpaEntity> stepEntities = new ArrayList<>();
        for (AssemblyStep step : domain.getSteps()) {
            AssemblyStepJpaEntity stepEntity = new AssemblyStepJpaEntity();
            stepEntity.setId(step.getId().value());
            stepEntity.setAssemblyProcess(entity);
            stepEntity.setWorkStationCode(step.getWorkStation().code());
            stepEntity.setWorkStationSequence(step.getWorkStation().sequence());
            stepEntity.setTaskDescription(step.getTaskDescription());
            stepEntity.setStandardTimeMinutes(step.getStandardTimeMinutes());
            stepEntity.setStatus(step.getStatus().name());
            stepEntity.setOperatorId(step.getOperatorId());
            stepEntity.setMaterialBatchId(
                step.getMaterialBatchId() != null ? step.getMaterialBatchId().value() : null);
            stepEntity.setActualTimeMinutes(step.getActualTimeMinutes());
            stepEntity.setCompletedAt(step.getCompletedAt());
            stepEntities.add(stepEntity);
        }
        entity.setSteps(stepEntities);

        return entity;
    }

    private AssemblyProcess mapAssemblyProcessToDomain(AssemblyProcessJpaEntity entity) {
        AssemblyProcessId processId = new AssemblyProcessId(entity.getId());
        AssemblyProcessStatus processStatus = AssemblyProcessStatus.valueOf(entity.getStatus());

        List<AssemblyStep> steps = entity.getSteps().stream()
            .map(stepEntity -> {
                AssemblyStepId stepId = new AssemblyStepId(stepEntity.getId());
                WorkStationId workStation = new WorkStationId(
                    stepEntity.getWorkStationCode(), stepEntity.getWorkStationSequence());
                AssemblyStepStatus stepStatus = AssemblyStepStatus.valueOf(stepEntity.getStatus());
                MaterialBatchId batchId = stepEntity.getMaterialBatchId() != null
                    ? new MaterialBatchId(stepEntity.getMaterialBatchId()) : null;

                return AssemblyStep.reconstitute(
                    stepId, workStation, stepEntity.getTaskDescription(),
                    stepEntity.getStandardTimeMinutes(), stepStatus,
                    stepEntity.getOperatorId(), batchId,
                    stepEntity.getActualTimeMinutes(), stepEntity.getCompletedAt()
                );
            })
            .toList();

        return AssemblyProcess.reconstitute(processId, processStatus, steps);
    }
}
