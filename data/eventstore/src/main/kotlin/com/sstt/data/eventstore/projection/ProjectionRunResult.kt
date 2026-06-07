package com.sstt.data.eventstore.projection

/**
 * Summary of one projection runner pass.
 *
 * [processedEvents] counts all observed global events, including events ignored
 * by a projection. [lastGlobalPosition] is the final durable position saved for
 * the projection after the pass.
 */
data class ProjectionRunResult(
    val projectionName: String,
    val processedEvents: Long,
    val lastGlobalPosition: Long,
)
