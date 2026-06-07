package com.sstt.data.eventstore.projection

import com.sstt.data.eventstore.model.StoredEvent
import reactor.core.publisher.Mono

/**
 * Reusable contract for a read model projection fed by the global event log.
 *
 * A projection owns one rebuildable read model and advances independently from
 * other projections through its durable offset. Implementations should be
 * idempotent because a failure after applying an event but before saving the
 * offset will cause the same event to be replayed.
 */
interface EventProjection {
    /**
     * Stable projection name used as the key in projection offset storage.
     */
    val name: String

    /**
     * Applies one stored event to the projection read model.
     *
     * Implementations should return [Mono.empty] for events they do not handle.
     * The runner will still advance the projection offset after this method
     * completes successfully, because the projection has observed that global
     * event position.
     */
    fun apply(event: StoredEvent): Mono<Void>
}
