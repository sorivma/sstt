package com.sstt.data.eventstore.api

import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.StoredEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Reactive append-only store for generic JSON domain events.
 *
 * This interface is the storage boundary for event sourcing. It does not expose
 * domain-specific event classes and does not build read models. Its job is to
 * append events atomically, preserve event order, enforce stream-level
 * optimistic concurrency, and provide replay APIs for aggregates and
 * projections.
 */
interface EventStore {
    /**
     * Atomically appends all events from [command] to one stream.
     *
     * Implementations must assign consecutive stream versions, assign global
     * positions, and fail the operation if [AppendEventsCommand.expectedVersion]
     * does not match the current stream state.
     */
    fun append(command: AppendEventsCommand): Mono<AppendEventsResult>

    /**
     * Reads every event in a stream ordered by [StoredEvent.streamVersion].
     *
     * Aggregate reconstruction should use this method because stream version is
     * the local order of events within a single aggregate instance.
     */
    fun loadStream(streamName: String): Flux<StoredEvent>

    /**
     * Reads the global event log after [globalPosition], ordered by
     * [StoredEvent.globalPosition], up to [limit] events.
     *
     * Projection runners should use this method and persist their progress in a
     * [com.sstt.data.eventstore.api.ProjectionOffsetStore]. Offsets belong to
     * projections, not to streams.
     */
    fun readFrom(globalPosition: Long, limit: Int): Flux<StoredEvent>
}
