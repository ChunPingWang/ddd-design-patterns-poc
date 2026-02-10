package com.automfg.shared.application;

/**
 * Marker interface for all use cases (both commands and queries).
 * Part of the CQRS pattern: every use case is either a {@link CommandUseCase}
 * or a {@link QueryUseCase}, never both.
 */
public interface UseCase {
}
