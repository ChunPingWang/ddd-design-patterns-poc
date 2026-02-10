package com.automfg.shared.application;

/**
 * Marker interface for command use cases — operations that mutate state.
 * Commands go through the domain model, enforce invariants, and may emit domain events.
 *
 * <p>Convention: Each CommandUseCase defines:
 * <ul>
 *   <li>An inner record named *Command (the input)</li>
 *   <li>An inner record named *Result (the output)</li>
 *   <li>A method: {@code Result execute(Command)}</li>
 * </ul>
 */
public interface CommandUseCase extends UseCase {
}
