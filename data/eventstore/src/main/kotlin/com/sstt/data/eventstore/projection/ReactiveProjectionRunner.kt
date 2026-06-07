package com.sstt.data.eventstore.projection

import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.api.ProjectionOffsetStore
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Reactive polling runner for rebuildable read model projections.
 *
 * The runner reads the global event log from each projection's durable offset,
 * applies events sequentially with [Flux.concatMap], and saves the new offset
 * only after projection application succeeds. This makes the runner resilient to
 * process restarts and safe for idempotent projections.
 */
class ReactiveProjectionRunner(
    private val eventStore: EventStore,
    private val offsetStore: ProjectionOffsetStore,
    private val projections: List<EventProjection>,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        require(batchSize > 0) { "batchSize must be > 0" }
        require(projections.map { it.name }.toSet().size == projections.size) {
            "projection names must be unique"
        }
    }

    /**
     * Runs all registered projections once and emits one result per projection.
     */
    fun runAllOnce(): Flux<ProjectionRunResult> {
        return Flux.fromIterable(projections)
            .concatMap { projection -> runOnce(projection) }
    }

    /**
     * Runs the projection with [projectionName] once.
     */
    fun runOnce(projectionName: String): Mono<ProjectionRunResult> {
        val projection = projections.firstOrNull { it.name == projectionName }
            ?: return Mono.error(IllegalArgumentException("Unknown projection '$projectionName'"))

        return runOnce(projection)
    }

    /**
     * Starts a reactive polling loop. The returned [Flux] is cold; projection
     * processing starts when the caller subscribes and stops when subscription is
     * cancelled.
     */
    fun runContinuously(interval: Duration): Flux<ProjectionRunResult> {
        require(!interval.isNegative && !interval.isZero) { "interval must be positive" }

        return Flux.interval(Duration.ZERO, interval)
            .concatMap { runAllOnce() }
    }

    private fun runOnce(projection: EventProjection): Mono<ProjectionRunResult> {
        return offsetStore.load(projection.name)
            .flatMapMany { offset ->
                eventStore.readFrom(offset.lastGlobalPosition, batchSize)
            }
            .concatMap { event ->
                projection.apply(event)
                    .then(offsetStore.save(projection.name, event.globalPosition))
                    .thenReturn(event.globalPosition)
            }
            .reduce(
                Accumulator(processedEvents = 0, lastGlobalPosition = 0),
            ) { accumulator, globalPosition ->
                Accumulator(
                    processedEvents = accumulator.processedEvents + 1,
                    lastGlobalPosition = globalPosition,
                )
            }
            .defaultIfEmpty(Accumulator(processedEvents = 0, lastGlobalPosition = 0))
            .map { accumulator ->
                ProjectionRunResult(
                    projectionName = projection.name,
                    processedEvents = accumulator.processedEvents,
                    lastGlobalPosition = accumulator.lastGlobalPosition,
                )
            }
    }

    private data class Accumulator(
        val processedEvents: Long,
        val lastGlobalPosition: Long,
    )

    companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }
}
