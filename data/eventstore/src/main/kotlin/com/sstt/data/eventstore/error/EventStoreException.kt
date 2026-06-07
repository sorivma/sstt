package com.sstt.data.eventstore.error

/**
 * Base runtime exception for event store failures that are meaningful to
 * application code.
 *
 * Infrastructure exceptions from the database driver can still surface when the
 * storage layer is unavailable. Subclasses of this type represent event store
 * contract violations such as failed optimistic concurrency checks.
 */
open class EventStoreException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
