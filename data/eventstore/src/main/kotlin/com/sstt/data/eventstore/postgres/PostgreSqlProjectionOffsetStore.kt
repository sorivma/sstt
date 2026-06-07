package com.sstt.data.eventstore.postgres

import com.sstt.data.eventstore.api.ProjectionOffsetStore
import com.sstt.data.eventstore.model.ProjectionOffset
import io.r2dbc.spi.Readable
import java.time.Clock
import java.time.Instant
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

/**
 * PostgreSQL/R2DBC implementation of [ProjectionOffsetStore].
 *
 * Offsets are stored independently per projection. Saving an offset uses an
 * upsert so projection runners can create their checkpoint row on first
 * successful batch and then advance it after each committed batch.
 */
class PostgreSqlProjectionOffsetStore(
    private val databaseClient: DatabaseClient,
    private val clock: Clock = Clock.systemUTC(),
) : ProjectionOffsetStore {
    override fun load(projectionName: String): Mono<ProjectionOffset> {
        require(projectionName.isNotBlank()) { "projectionName must not be blank" }

        return databaseClient.sql(
            """
            select projection_name, last_global_position, updated_at
            from eventstore.projection_offsets
            where projection_name = :projection_name
            """.trimIndent(),
        )
            .bind("projection_name", projectionName)
            .map { row -> mapProjectionOffset(row) }
            .one()
            .switchIfEmpty(
                Mono.just(
                    ProjectionOffset(
                        projectionName = projectionName,
                        lastGlobalPosition = 0,
                        updatedAt = Instant.EPOCH,
                    ),
                ),
            )
    }

    override fun save(projectionName: String, globalPosition: Long): Mono<Void> {
        require(projectionName.isNotBlank()) { "projectionName must not be blank" }
        require(globalPosition >= 0) { "globalPosition must be >= 0" }

        return databaseClient.sql(
            """
            insert into eventstore.projection_offsets (
                projection_name,
                last_global_position,
                updated_at
            )
            values (
                :projection_name,
                :last_global_position,
                :updated_at
            )
            on conflict (projection_name) do update
            set last_global_position = excluded.last_global_position,
                updated_at = excluded.updated_at
            """.trimIndent(),
        )
            .bind("projection_name", projectionName)
            .bind("last_global_position", globalPosition)
            .bind("updated_at", Instant.now(clock))
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun mapProjectionOffset(row: Readable): ProjectionOffset {
        return ProjectionOffset(
            projectionName = row.require("projection_name", String::class.java),
            lastGlobalPosition = row.require("last_global_position", java.lang.Long::class.java).toLong(),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
