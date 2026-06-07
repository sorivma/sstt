package com.sstt.data.eventstore.model

/**
 * Runtime configuration contract for event store adapters.
 *
 * The configuration lives in the public model package so application wiring can
 * declare event store behavior without depending on a concrete database
 * implementation. PostgreSQL currently supports strict commit order only,
 * because global positions are used as durable projection cursors.
 */
data class EventStoreConfiguration(
    val strictCommitOrder: Boolean = true,
    val defaultReadBatchSize: Int = 500,
) {
    init {
        require(strictCommitOrder) { "strictCommitOrder=false is not supported by the current PostgreSQL adapter" }
        require(defaultReadBatchSize > 0) { "defaultReadBatchSize must be > 0" }
    }
}
