package com.automfg.manufacturing.domain.model;

public enum ProductionOrderStatus {
    MATERIAL_PENDING,
    SCHEDULED,
    IN_PRODUCTION,
    ASSEMBLY_COMPLETED,
    INSPECTION_PASSED,
    INSPECTION_FAILED,
    REWORK_IN_PROGRESS
}
