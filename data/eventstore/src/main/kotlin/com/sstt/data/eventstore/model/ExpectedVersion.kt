package com.sstt.data.eventstore.model

/**
 * Expected stream version used for optimistic concurrency during append.
 *
 * Event sourcing commands normally load a stream, decide which events should be
 * produced, and append them only if nobody changed that stream in the meantime.
 * This type makes that expectation explicit in the append contract.
 */
sealed interface ExpectedVersion {
    /**
     * Appends without checking the current stream version.
     *
     * This is useful for infrastructure workflows, imports, or intentionally
     * idempotent commands, but command handlers should prefer [NoStream] or
     * [Exact] when they are enforcing aggregate invariants.
     */
    data object Any : ExpectedVersion

    /**
     * Requires the stream to be absent before the append.
     *
     * This is the expected version for creating a new aggregate stream.
     */
    data object NoStream : ExpectedVersion

    /**
     * Requires the stream to exist at exactly [value] before the append.
     *
     * The first stored event in a stream has version `1`, so an aggregate loaded
     * with three events should append with `Exact(3)`.
     */
    data class Exact(val value: Long) : ExpectedVersion {
        init {
            require(value >= 0) { "expected stream version must be >= 0" }
        }
    }
}
