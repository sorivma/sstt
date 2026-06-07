package com.sstt.data.eventstore.error

import com.sstt.data.eventstore.model.ExpectedVersion

/**
 * Raised when an append command observes a different stream version than the
 * version it declared in [ExpectedVersion].
 *
 * Command handlers should treat this as a normal optimistic concurrency
 * conflict: reload the stream, rebuild aggregate state, and decide whether the
 * command is still valid.
 */
class StreamConcurrencyException(
    message: String,
) : EventStoreException(message)
