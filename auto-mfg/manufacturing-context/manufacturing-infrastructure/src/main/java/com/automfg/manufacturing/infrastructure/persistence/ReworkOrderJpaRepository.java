package com.automfg.manufacturing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReworkOrderJpaRepository extends JpaRepository<ReworkOrderJpaEntity, UUID> {
}
