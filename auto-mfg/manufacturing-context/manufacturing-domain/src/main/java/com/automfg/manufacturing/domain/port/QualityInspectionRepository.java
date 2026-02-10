package com.automfg.manufacturing.domain.port;

import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.model.QualityInspectionId;

import java.util.Optional;

public interface QualityInspectionRepository {
    QualityInspection save(QualityInspection inspection);
    Optional<QualityInspection> findById(QualityInspectionId id);
}
