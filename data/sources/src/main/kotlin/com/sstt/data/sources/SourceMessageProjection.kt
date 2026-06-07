package com.sstt.data.sources

import com.fasterxml.jackson.databind.JsonNode
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import com.sstt.data.sources.events.SourceEventTypes
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class SourceMessageProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            SourceEventTypes.SOURCE_MESSAGE_ADDED -> applyAdded(event)
            SourceEventTypes.SOURCE_MESSAGE_PROCESSED -> applyStatus(event, "PROCESSED", "processing_summary")
            SourceEventTypes.SOURCE_MESSAGE_FAILED -> applyStatus(event, "FAILED", "failure_reason")
            else -> Mono.empty()
        }
    }

    private fun applyAdded(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into sources.source_messages (
                message_id, student_id, source_type, content, status, processing_summary,
                failure_reason, stream_version, created_at, updated_at
            )
            values (
                :message_id, :student_id, :source_type, :content, 'NEW', null,
                null, :stream_version, :created_at, :updated_at
            )
            on conflict (message_id) do update
            set source_type = excluded.source_type,
                content = excluded.content,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where sources.source_messages.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("message_id", uuid(event.payload["message_id"]))
            .bind("student_id", uuid(event.payload["student_id"]))
            .bind("source_type", event.payload["source_type"].asText())
            .bind("content", event.payload["content"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyStatus(event: StoredEvent, status: String, detailColumn: String): Mono<Void> {
        return databaseClient.sql(
            """
            update sources.source_messages
            set status = :status,
                $detailColumn = :detail,
                stream_version = :stream_version,
                updated_at = :updated_at
            where message_id = :message_id and stream_version < :stream_version
            """.trimIndent(),
        )
            .bind("message_id", uuid(event.payload["message_id"]))
            .bind("status", status)
            .bindNullableText("detail", event.payload[detailColumn]?.takeUnless { it.isNull }?.asText())
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
        const val PROJECTION_NAME = "sources.source_messages"
    }
}
