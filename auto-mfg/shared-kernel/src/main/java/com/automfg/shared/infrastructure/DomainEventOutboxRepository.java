package com.automfg.shared.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DomainEventOutboxRepository extends JpaRepository<DomainEventOutbox, UUID> {

    List<DomainEventOutbox> findByPublishedAtIsNullOrderByCreatedAtAsc();
}
