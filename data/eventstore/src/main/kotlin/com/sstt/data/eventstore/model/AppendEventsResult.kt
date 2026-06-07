package com.sstt.data.eventstore.model

/**
 * Result of a successful append operation.
 *
 * The result reports the stream version before and after the append, plus the
 * fully persisted [StoredEvent] records with assigned identifiers, stream
 * versions, timestamps, and global positions. Callers can use this object to
 * publish in-process notifications or continue a workflow without re-reading the
 * stream from storage.
 */
data class AppendEventsResult(
    val streamName: String,
    val streamType: String,
    val previousStreamVersion: Long,
    val currentStreamVersion: Long,
    val events: List<StoredEvent>,
)
