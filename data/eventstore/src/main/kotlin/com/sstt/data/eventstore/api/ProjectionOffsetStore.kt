package com.sstt.data.eventstore.api

import com.sstt.data.eventstore.model.ProjectionOffset
import reactor.core.publisher.Mono

/**
 * Reactive storage for projection checkpoints.
 *
 * Projection runners use this store to resume from the last fully processed
 * global event position. The offset is intentionally keyed by projection name,
 * because stream ordering is already represented by stream versions and
 * projection progress is about the global log.
 */
interface ProjectionOffsetStore {
    /**
     * Loads the checkpoint for [projectionName].
     */
    fun load(projectionName: String): Mono<ProjectionOffset>

    /**
     * Persists that [projectionName] has fully processed events through
     * [globalPosition].
     */
    fun save(projectionName: String, globalPosition: Long): Mono<Void>
}
