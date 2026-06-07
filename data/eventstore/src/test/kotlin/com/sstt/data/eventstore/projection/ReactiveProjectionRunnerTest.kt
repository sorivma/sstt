package com.sstt.data.eventstore.projection

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.api.ProjectionOffsetStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ProjectionOffset
import com.sstt.data.eventstore.model.StoredEvent
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for the reusable reactive projection runner contract.
 */
class ReactiveProjectionRunnerTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `run once applies events sequentially and saves offsets`() {
        val events = listOf(
            storedEvent(globalPosition = 1, eventType = "First"),
            storedEvent(globalPosition = 2, eventType = "Second"),
        )
        val eventStore = InMemoryEventStore(events)
        val offsetStore = InMemoryOffsetStore()
        val projection = CapturingProjection("test.projection")
        val runner = ReactiveProjectionRunner(eventStore, offsetStore, listOf(projection), batchSize = 10)

        StepVerifier.create(runner.runOnce("test.projection"))
            .assertNext { result ->
                check(result.projectionName == "test.projection")
                check(result.processedEvents == 2L)
                check(result.lastGlobalPosition == 2L)
            }
            .verifyComplete()

        check(projection.appliedEventTypes == listOf("First", "Second"))
        check(offsetStore.savedOffsets == listOf(1L, 2L))
    }

    @Test
    fun `run once advances offset for ignored events`() {
        val eventStore = InMemoryEventStore(listOf(storedEvent(globalPosition = 5, eventType = "Ignored")))
        val offsetStore = InMemoryOffsetStore()
        val projection = IgnoringProjection("test.projection")
        val runner = ReactiveProjectionRunner(eventStore, offsetStore, listOf(projection), batchSize = 10)

        StepVerifier.create(runner.runOnce("test.projection"))
            .assertNext { result ->
                check(result.processedEvents == 1L)
                check(result.lastGlobalPosition == 5L)
            }
            .verifyComplete()

        check(offsetStore.savedOffsets == listOf(5L))
    }

    @Test
    fun `run once rejects unknown projection`() {
        val runner = ReactiveProjectionRunner(
            eventStore = InMemoryEventStore(emptyList()),
            offsetStore = InMemoryOffsetStore(),
            projections = emptyList(),
        )

        StepVerifier.create(runner.runOnce("missing"))
            .expectError(IllegalArgumentException::class.java)
            .verify()
    }

    private fun storedEvent(globalPosition: Long, eventType: String): StoredEvent {
        return StoredEvent(
            eventId = UUID.randomUUID(),
            globalPosition = globalPosition,
            streamId = UUID.randomUUID(),
            streamName = "test:1",
            streamType = "test",
            streamVersion = globalPosition,
            eventType = eventType,
            eventVersion = 1,
            occurredAt = Instant.parse("2026-06-07T12:00:00Z"),
            actorId = null,
            correlationId = null,
            causationId = null,
            payload = objectMapper.createObjectNode(),
            metadata = objectMapper.createObjectNode(),
        )
    }

    private class InMemoryEventStore(
        private val events: List<StoredEvent>,
    ) : EventStore {
        override fun append(command: AppendEventsCommand): Mono<AppendEventsResult> {
            return Mono.error(UnsupportedOperationException("append is not used by this test"))
        }

        override fun loadStream(streamName: String): Flux<StoredEvent> {
            return Flux.fromIterable(events.filter { it.streamName == streamName })
        }

        override fun readFrom(globalPosition: Long, limit: Int): Flux<StoredEvent> {
            return Flux.fromIterable(
                events
                    .filter { it.globalPosition > globalPosition }
                    .sortedBy { it.globalPosition }
                    .take(limit),
            )
        }
    }

    private class InMemoryOffsetStore : ProjectionOffsetStore {
        val savedOffsets = mutableListOf<Long>()
        private var currentOffset = 0L

        override fun load(projectionName: String): Mono<ProjectionOffset> {
            return Mono.just(
                ProjectionOffset(
                    projectionName = projectionName,
                    lastGlobalPosition = currentOffset,
                    updatedAt = Instant.EPOCH,
                ),
            )
        }

        override fun save(projectionName: String, globalPosition: Long): Mono<Void> {
            savedOffsets += globalPosition
            currentOffset = globalPosition
            return Mono.empty()
        }
    }

    private class CapturingProjection(
        override val name: String,
    ) : EventProjection {
        val appliedEventTypes = mutableListOf<String>()

        override fun apply(event: StoredEvent): Mono<Void> {
            appliedEventTypes += event.eventType
            return Mono.empty()
        }
    }

    private class IgnoringProjection(
        override val name: String,
    ) : EventProjection {
        override fun apply(event: StoredEvent): Mono<Void> = Mono.empty()
    }
}
