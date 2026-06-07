package com.sstt.data.eventstore.model

import java.util.UUID

/**
 * Request to append one or more generic JSON events to a single event stream.
 *
 * A stream represents the history of one aggregate instance, for example
 * `student:<id>` or `task:<id>`. The event store treats the payload as opaque
 * JSON and is responsible only for ordering, concurrency checks, and durable
 * persistence. Domain modules own the meaning of [streamType], [eventType], and
 * the JSON shape inside each [NewEvent].
 *
 * [expectedVersion] is the optimistic concurrency guard. It prevents command
 * handlers from accidentally appending events based on stale aggregate state.
 * [actorId], [correlationId], and [causationId] are copied to every stored event
 * in this append operation so later audits can reconstruct who caused a change
 * and which workflow it belonged to.
 */
data class AppendEventsCommand(
    val streamName: String,
    val streamType: String,
    val expectedVersion: ExpectedVersion,
    val events: List<NewEvent>,
    val actorId: UUID? = null,
    val correlationId: UUID? = null,
    val causationId: UUID? = null,
) {
    init {
        require(streamName.isNotBlank()) { "streamName must not be blank" }
        require(streamType.isNotBlank()) { "streamType must not be blank" }
        require(events.isNotEmpty()) { "events must not be empty" }
        require(events.map { it.eventId }.toSet().size == events.size) {
            "events must have unique event ids"
        }
    }
}
