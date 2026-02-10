package com.automfg.shared.application;

/**
 * Marker interface for query use cases — operations that only read data, never mutate state.
 * Queries may bypass the domain model and read directly from persistence for efficiency.
 *
 * <p>Convention: Each QueryUseCase defines:
 * <ul>
 *   <li>An inner record named *Query (the input)</li>
 *   <li>An inner record or type for the result (the output)</li>
 *   <li>A method: {@code Result execute(Query)}</li>
 * </ul>
 *
 * <p>CQRS rule: Query use cases must NOT publish domain events or mutate aggregate state.
 */
public interface QueryUseCase extends UseCase {
}
