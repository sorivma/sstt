package com.sstt.data.materials

import com.fasterxml.jackson.databind.JsonNode
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import com.sstt.data.materials.events.MaterialEventTypes
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class MaterialProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            MaterialEventTypes.MATERIAL_REGISTERED -> applyRegistered(event)
            MaterialEventTypes.MATERIAL_SUMMARY_CHANGED -> applySummaryChanged(event)
            else -> Mono.empty()
        }
    }

    private fun applyRegistered(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into materials.materials (
                material_id, student_id, file_name, content_type, storage_key, summary,
                stream_version, created_at, updated_at
            )
            values (
                :material_id, :student_id, :file_name, :content_type, :storage_key, null,
                :stream_version, :created_at, :updated_at
            )
            on conflict (material_id) do update
            set file_name = excluded.file_name,
                content_type = excluded.content_type,
                storage_key = excluded.storage_key,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where materials.materials.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("material_id", uuid(event.payload["material_id"]))
            .bind("student_id", uuid(event.payload["student_id"]))
            .bind("file_name", event.payload["file_name"].asText())
            .bindNullableText("content_type", event.payload["content_type"]?.takeUnless { it.isNull }?.asText())
            .bind("storage_key", event.payload["storage_key"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applySummaryChanged(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            update materials.materials
            set summary = :summary, stream_version = :stream_version, updated_at = :updated_at
            where material_id = :material_id and stream_version < :stream_version
            """.trimIndent(),
        )
            .bind("material_id", uuid(event.payload["material_id"]))
            .bind("summary", event.payload["summary"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun uuid(node: JsonNode): UUID = UUID.fromString(node.asText())

    private fun DatabaseClient.GenericExecuteSpec.bindNullableText(name: String, value: String?): DatabaseClient.GenericExecuteSpec {
        return if (value == null) bindNull(name, String::class.java) else bind(name, value)
    }

    companion object {
        const val PROJECTION_NAME = "materials.materials"
    }
}
