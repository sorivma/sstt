package com.sstt.data.eventstore.model

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

/**
 * Event after it has been committed to the event store.
 *
 * [globalPosition] is the monotonic cursor of the whole event log and is used by
 * projections. [streamVersion] is the local event number inside one aggregate
 * stream and is used for aggregate reconstruction and concurrency checks.
 * [payload] and [metadata] remain generic JSON so the event store is not coupled
 * to domain-specific Kotlin event classes.
 */
data class StoredEvent(
    val eventId: UUID,
    val globalPosition: Long,
    val streamId: UUID,
    val streamName: String,
    val streamType: String,
    val streamVersion: Long,
    val eventType: String,
    val eventVersion: Int,
    val occurredAt: Instant,
    val actorId: UUID?,
    val correlationId: UUID?,
    val causationId: UUID?,
    val payload: JsonNode,
    val metadata: JsonNode,
)
