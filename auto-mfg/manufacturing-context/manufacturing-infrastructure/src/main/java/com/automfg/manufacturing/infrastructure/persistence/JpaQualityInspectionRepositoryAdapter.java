package com.automfg.manufacturing.infrastructure.persistence;

import com.automfg.manufacturing.domain.model.QualityInspection;
import com.automfg.manufacturing.domain.model.QualityInspectionId;
import com.automfg.manufacturing.domain.port.QualityInspectionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaQualityInspectionRepositoryAdapter implements QualityInspectionRepository {

    private final QualityInspectionJpaRepository jpaRepository;

    public JpaQualityInspectionRepositoryAdapter(QualityInspectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public QualityInspection save(QualityInspection inspection) {
        QualityInspectionJpaEntity entity = QualityInspectionMapper.toJpaEntity(inspection);
        QualityInspectionJpaEntity saved = jpaRepository.save(entity);
        return QualityInspectionMapper.toDomain(saved);
    }

    @Override
    public Optional<QualityInspection> findById(QualityInspectionId id) {
        return jpaRepository.findById(id.value())
            .map(QualityInspectionMapper::toDomain);
    }
}
