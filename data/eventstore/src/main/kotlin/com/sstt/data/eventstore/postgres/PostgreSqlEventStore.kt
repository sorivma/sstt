package com.sstt.data.eventstore.postgres

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.error.EventStoreException
import com.sstt.data.eventstore.error.StreamConcurrencyException
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.EventStoreConfiguration
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import com.sstt.data.eventstore.model.StoredEvent
import io.r2dbc.postgresql.codec.Json
import io.r2dbc.spi.Readable
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * PostgreSQL/R2DBC implementation of [EventStore].
 *
 * The implementation stores all event types in one global append-only table and
 * preserves two orders at the same time: [StoredEvent.globalPosition] for the
 * whole log and [StoredEvent.streamVersion] for one aggregate stream. Appends
 * run inside a reactive transaction so stream registration, optimistic
 * concurrency validation, event inserts, and stream version updates commit or
 * roll back together.
 */
class PostgreSqlEventStore(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator,
    private val objectMapper: ObjectMapper,
    private val configuration: EventStoreConfiguration = EventStoreConfiguration(),
    private val clock: Clock = Clock.systemUTC(),
    private val streamIdGenerator: () -> UUID = UUID::randomUUID,
) : EventStore {
    override fun append(command: AppendEventsCommand): Mono<AppendEventsResult> {
        validateCommand(command)

        return Mono.defer { appendInTransaction(command) }
            .`as`(transactionalOperator::transactional)
    }

    override fun loadStream(streamName: String): Flux<StoredEvent> {
        require(streamName.isNotBlank()) { "streamName must not be blank" }

        return databaseClient.sql(
            """
            select *
            from eventstore.events
            where stream_name = :stream_name
            order by stream_version
            """.trimIndent(),
        )
            .bind("stream_name", streamName)
            .map { row -> mapStoredEvent(row) }
            .all()
    }

    override fun readFrom(globalPosition: Long, limit: Int): Flux<StoredEvent> {
        require(globalPosition >= 0) { "globalPosition must be >= 0" }
        require(limit > 0) { "limit must be > 0" }

        return databaseClient.sql(
            """
            select *
            from eventstore.events
            where global_position > :global_position
            order by global_position
            limit :limit
            """.trimIndent(),
        )
            .bind("global_position", globalPosition)
            .bind("limit", limit)
            .map { row -> mapStoredEvent(row) }
            .all()
    }

    private fun appendInTransaction(command: AppendEventsCommand): Mono<AppendEventsResult> {
        return findExistingEvents(command.events.map { it.eventId })
            .collectList()
            .flatMap { existingEvents ->
                if (existingEvents.isNotEmpty()) {
                    return@flatMap resolveIdempotentAppend(command, existingEvents)
                }

                appendNewEvents(command)
            }
    }

    private fun appendNewEvents(command: AppendEventsCommand): Mono<AppendEventsResult> {
        return loadStreamRecordForUpdate(command.streamName)
            .switchIfEmpty(createStream(command))
            .flatMap { stream ->
                validateExpectedVersion(command, stream)

                val previousVersion = stream.currentVersion
                val occurredAt = Instant.now(clock)
                allocateGlobalPositions(command.events.size)
                    .flatMapMany { firstGlobalPosition ->
                        val storedEvents = command.events.mapIndexed { index, event ->
                            PendingStoredEvent(
                                globalPosition = firstGlobalPosition + index,
                                stream = stream,
                                streamVersion = previousVersion + index + 1,
                                event = event,
                                occurredAt = occurredAt,
                            )
                        }

                        Flux.fromIterable(storedEvents)
                            .concatMap { insertEvent(command, it) }
                    }
                    .collectList()
                    .flatMap { insertedEvents ->
                        updateStreamVersion(stream, previousVersion + command.events.size)
                            .thenReturn(
                                AppendEventsResult(
                                    streamName = command.streamName,
                                    streamType = command.streamType,
                                    previousStreamVersion = previousVersion,
                                    currentStreamVersion = previousVersion + command.events.size,
                                    events = insertedEvents,
                                ),
                            )
                    }
            }
    }

    private fun validateCommand(command: AppendEventsCommand) {
        check(configuration.strictCommitOrder) {
            "PostgreSqlEventStore requires strictCommitOrder=true"
        }

        val duplicateEventIds = command.events
            .groupBy { it.eventId }
            .filterValues { it.size > 1 }
            .keys

        require(duplicateEventIds.isEmpty()) {
            "events contain duplicate event ids: $duplicateEventIds"
        }
    }

    private fun findExistingEvents(eventIds: List<UUID>): Flux<StoredEvent> {
        return databaseClient.sql(
            """
            select *
            from eventstore.events
            where event_id = any(:event_ids)
            order by global_position
            """.trimIndent(),
        )
            .bind("event_ids", eventIds.toTypedArray())
            .map { row -> mapStoredEvent(row) }
            .all()
    }

    private fun resolveIdempotentAppend(
        command: AppendEventsCommand,
        existingEvents: List<StoredEvent>,
    ): Mono<AppendEventsResult> {
        val requestedEventIds = command.events.map { it.eventId }
        val existingById = existingEvents.associateBy { it.eventId }

        if (existingById.keys != requestedEventIds.toSet()) {
            return Mono.error(
                EventStoreException(
                    "Only some events from append command already exist; refusing partial idempotent append.",
                ),
            )
        }

        val orderedEvents = requestedEventIds.map { eventId -> existingById.getValue(eventId) }
        val firstEvent = orderedEvents.first()
        val lastEvent = orderedEvents.last()

        val matchesCommand = orderedEvents.zip(command.events).all { (stored, requested) ->
            stored.streamName == command.streamName &&
                stored.streamType == command.streamType &&
                stored.eventType == requested.eventType &&
                stored.eventVersion == requested.eventVersion &&
                stored.payload == requested.payload &&
                stored.metadata == requested.metadata
        }

        if (!matchesCommand) {
            return Mono.error(
                EventStoreException(
                    "Existing event ids do not match append command payload; refusing idempotent append.",
                ),
            )
        }

        return Mono.just(
            AppendEventsResult(
                streamName = command.streamName,
                streamType = command.streamType,
                previousStreamVersion = firstEvent.streamVersion - 1,
                currentStreamVersion = lastEvent.streamVersion,
                events = orderedEvents,
            ),
        )
    }

    private fun loadStreamRecordForUpdate(streamName: String): Mono<StreamRecord> {
        return databaseClient.sql(
            """
            select stream_id, stream_name, stream_type, current_version
            from eventstore.streams
            where stream_name = :stream_name
            for update
            """.trimIndent(),
        )
            .bind("stream_name", streamName)
            .map { row -> mapStreamRecord(row) }
            .one()
    }

    private fun createStream(command: AppendEventsCommand): Mono<StreamRecord> {
        if (command.expectedVersion is ExpectedVersion.Exact) {
            return Mono.error(
                StreamConcurrencyException(
                    "Stream '${command.streamName}' does not exist; expected version ${command.expectedVersion.value}.",
                ),
            )
        }

        val streamId = streamIdGenerator()
        val now = Instant.now(clock)

        return databaseClient.sql(
            """
            insert into eventstore.streams (
                stream_id,
                stream_name,
                stream_type,
                current_version,
                created_at,
                updated_at
            )
            values (
                :stream_id,
                :stream_name,
                :stream_type,
                0,
                :created_at,
                :updated_at
            )
            returning stream_id, stream_name, stream_type, current_version
            """.trimIndent(),
        )
            .bind("stream_id", streamId)
            .bind("stream_name", command.streamName)
            .bind("stream_type", command.streamType)
            .bind("created_at", now)
            .bind("updated_at", now)
            .map { row -> mapStreamRecord(row) }
            .one()
    }

    private fun validateExpectedVersion(command: AppendEventsCommand, stream: StreamRecord) {
        when (val expectedVersion = command.expectedVersion) {
            ExpectedVersion.Any -> Unit
            ExpectedVersion.NoStream -> {
                if (stream.currentVersion != 0L) {
                    throw StreamConcurrencyException(
                        "Stream '${command.streamName}' exists at version ${stream.currentVersion}; expected no stream.",
                    )
                }
            }
            is ExpectedVersion.Exact -> {
                if (stream.currentVersion != expectedVersion.value) {
                    throw StreamConcurrencyException(
                        "Stream '${command.streamName}' is at version ${stream.currentVersion}; expected ${expectedVersion.value}.",
                    )
                }
            }
        }

        if (stream.streamType != command.streamType) {
            throw EventStoreException(
                "Stream '${command.streamName}' has type '${stream.streamType}', but append requested '${command.streamType}'.",
            )
        }
    }

    private fun allocateGlobalPositions(eventCount: Int): Mono<Long> {
        return databaseClient.sql(
            """
            select last_position
            from eventstore.global_positions
            where position_name = 'events'
            for update
            """.trimIndent(),
        )
            .map { row -> row.require("last_position", java.lang.Long::class.java).toLong() }
            .one()
            .flatMap { lastPosition ->
                val firstPosition = lastPosition + 1
                val newLastPosition = lastPosition + eventCount

                databaseClient.sql(
                    """
                    update eventstore.global_positions
                    set last_position = :last_position
                    where position_name = 'events'
                    """.trimIndent(),
                )
                    .bind("last_position", newLastPosition)
                    .fetch()
                    .rowsUpdated()
                    .thenReturn(firstPosition)
            }
    }

    private fun insertEvent(command: AppendEventsCommand, pending: PendingStoredEvent): Mono<StoredEvent> {
        return databaseClient.sql(
            """
            insert into eventstore.events (
                global_position,
                event_id,
                stream_id,
                stream_name,
                stream_type,
                stream_version,
                event_type,
                event_version,
                occurred_at,
                actor_id,
                correlation_id,
                causation_id,
                payload,
                metadata
            )
            values (
                :global_position,
                :event_id,
                :stream_id,
                :stream_name,
                :stream_type,
                :stream_version,
                :event_type,
                :event_version,
                :occurred_at,
                :actor_id,
                :correlation_id,
                :causation_id,
                cast(:payload as jsonb),
                cast(:metadata as jsonb)
            )
            returning *
            """.trimIndent(),
        )
            .bind("global_position", pending.globalPosition)
            .bind("event_id", pending.event.eventId)
            .bind("stream_id", pending.stream.streamId)
            .bind("stream_name", pending.stream.streamName)
            .bind("stream_type", pending.stream.streamType)
            .bind("stream_version", pending.streamVersion)
            .bind("event_type", pending.event.eventType)
            .bind("event_version", pending.event.eventVersion)
            .bind("occurred_at", pending.occurredAt)
            .bindNullable("actor_id", command.actorId, UUID::class.java)
            .bindNullable("correlation_id", command.correlationId, UUID::class.java)
            .bindNullable("causation_id", command.causationId, UUID::class.java)
            .bind("payload", objectMapper.writeValueAsString(pending.event.payload))
            .bind("metadata", objectMapper.writeValueAsString(pending.event.metadata))
            .map { row -> mapStoredEvent(row) }
            .one()
    }

    private fun updateStreamVersion(stream: StreamRecord, currentVersion: Long): Mono<Void> {
        return databaseClient.sql(
            """
            update eventstore.streams
            set current_version = :current_version,
                updated_at = :updated_at
            where stream_id = :stream_id
            """.trimIndent(),
        )
            .bind("current_version", currentVersion)
            .bind("updated_at", Instant.now(clock))
            .bind("stream_id", stream.streamId)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun mapStreamRecord(row: Readable): StreamRecord {
        return StreamRecord(
            streamId = row.require("stream_id", UUID::class.java),
            streamName = row.require("stream_name", String::class.java),
            streamType = row.require("stream_type", String::class.java),
            currentVersion = row.require("current_version", java.lang.Long::class.java).toLong(),
        )
    }

    private fun mapStoredEvent(row: Readable): StoredEvent {
        return StoredEvent(
            eventId = row.require("event_id", UUID::class.java),
            globalPosition = row.require("global_position", java.lang.Long::class.java).toLong(),
            streamId = row.require("stream_id", UUID::class.java),
            streamName = row.require("stream_name", String::class.java),
            streamType = row.require("stream_type", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            eventType = row.require("event_type", String::class.java),
            eventVersion = row.require("event_version", java.lang.Integer::class.java).toInt(),
            occurredAt = row.require("occurred_at", Instant::class.java),
            actorId = row.get("actor_id", UUID::class.java),
            correlationId = row.get("correlation_id", UUID::class.java),
            causationId = row.get("causation_id", UUID::class.java),
            payload = row.readJson("payload"),
            metadata = row.readJson("metadata"),
        )
    }

    private fun Readable.readJson(column: String): JsonNode {
        return when (val value = get(column)) {
            is Json -> objectMapper.readTree(value.asString())
            is String -> objectMapper.readTree(value)
            is ByteArray -> objectMapper.readTree(value)
            null -> error("Column '$column' must not be null")
            else -> objectMapper.readTree(value.toString())
        }
    }

    private fun <T : Any> Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }

    private fun <T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: T?,
        type: Class<T>,
    ): DatabaseClient.GenericExecuteSpec {
        return if (value == null) {
            bindNull(name, type)
        } else {
            bind(name, value)
        }
    }

    private data class StreamRecord(
        val streamId: UUID,
        val streamName: String,
        val streamType: String,
        val currentVersion: Long,
    )

    private data class PendingStoredEvent(
        val globalPosition: Long,
        val stream: StreamRecord,
        val streamVersion: Long,
        val event: NewEvent,
        val occurredAt: Instant,
    )
}
