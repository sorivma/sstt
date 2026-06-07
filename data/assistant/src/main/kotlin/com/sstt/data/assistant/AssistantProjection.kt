package com.sstt.data.assistant

import com.fasterxml.jackson.databind.JsonNode
import com.sstt.data.assistant.events.AssistantEventTypes
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class AssistantProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            AssistantEventTypes.CHAT_THREAD_STARTED -> applyThreadStarted(event)
            AssistantEventTypes.CHAT_MESSAGE_APPENDED -> applyMessageAppended(event)
            else -> Mono.empty()
        }
    }

    private fun applyThreadStarted(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into projections.chat_threads (
                thread_id, student_id, title, last_message_at, stream_version, created_at, updated_at
            )
            values (
                :thread_id, :student_id, :title, null, :stream_version, :created_at, :updated_at
            )
            on conflict (thread_id) do update
            set title = excluded.title,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where projections.chat_threads.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("thread_id", uuid(event.payload["thread_id"]))
            .bind("student_id", uuid(event.payload["student_id"]))
            .bind("title", event.payload["title"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyMessageAppended(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into projections.chat_messages (message_id, thread_id, role, content, created_at)
            values (:message_id, :thread_id, :role, :content, :created_at)
            on conflict (message_id) do nothing
            """.trimIndent(),
        )
            .bind("message_id", uuid(event.payload["message_id"]))
            .bind("thread_id", uuid(event.payload["thread_id"]))
            .bind("role", event.payload["role"].asText())
            .bind("content", event.payload["content"].asText())
            .bind("created_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient.sql(
                    """
                    update projections.chat_threads
                    set last_message_at = :last_message_at,
                        stream_version = :stream_version,
                        updated_at = :updated_at
                    where thread_id = :thread_id and stream_version < :stream_version
                    """.trimIndent(),
                )
                    .bind("thread_id", uuid(event.payload["thread_id"]))
                    .bind("last_message_at", event.occurredAt)
                    .bind("stream_version", event.streamVersion)
                    .bind("updated_at", event.occurredAt)
                    .fetch()
                    .rowsUpdated()
                    .then(),
            )
    }

    private fun uuid(node: JsonNode): UUID = UUID.fromString(node.asText())

    companion object {
        const val PROJECTION_NAME = "projections.assistant_chat"
    }
}
