package com.automfg.manufacturing.application.usecase;

import com.automfg.shared.application.QueryUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GetAssemblyStepsUseCase extends QueryUseCase {

    record GetAssemblyStepsQuery(UUID productionOrderId, String stationCode) {}

    record AssemblyStepDetail(
        UUID id, String workStationCode, int workStationSequence,
        String taskDescription, int standardTimeMinutes,
        String status, String operatorId, String materialBatchId,
        Integer actualTimeMinutes, LocalDateTime completedAt
    ) {}

    List<AssemblyStepDetail> execute(GetAssemblyStepsQuery query);
}
