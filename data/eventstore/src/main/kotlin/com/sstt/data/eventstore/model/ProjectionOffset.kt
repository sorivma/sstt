package com.sstt.data.eventstore.model

import java.time.Instant

/**
 * Durable checkpoint for a read model projection.
 *
 * A projection processes the global event log in [StoredEvent.globalPosition]
 * order and stores the last position it has fully applied. This makes read
 * models rebuildable and lets each projection advance independently without
 * adding per-stream offsets.
 */
data class ProjectionOffset(
    val projectionName: String,
    val lastGlobalPosition: Long,
    val updatedAt: Instant,
)
