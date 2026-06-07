package com.sstt.data.eventstore.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

/**
 * Event prepared by a command handler before it has been persisted.
 *
 * [eventId] is assigned by the caller before append so retries can be
 * idempotent. [eventType] and [eventVersion] identify the domain event
 * contract. [payload] contains domain data as generic JSON, while [metadata]
 * contains technical or diagnostic context such as importer name, model name,
 * confidence score, or request information. The event store persists both JSON
 * documents unchanged.
 */
data class NewEvent(
    val eventId: UUID,
    val eventType: String,
    val eventVersion: Int,
    val payload: JsonNode,
    val metadata: JsonNode,
) {
    init {
        require(eventType.isNotBlank()) { "eventType must not be blank" }
        require(eventVersion > 0) { "eventVersion must be > 0" }
    }
}
