package com.automfg.shared.application;

/**
 * Marker interface for read models (query DTOs).
 * Read models are optimized for display and may denormalize data
 * that spans multiple aggregates — unlike domain objects which enforce invariants.
 */
public interface ReadModel {
}
