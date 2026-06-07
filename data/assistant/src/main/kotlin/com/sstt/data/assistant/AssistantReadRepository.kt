package com.sstt.data.assistant

import com.sstt.data.assistant.model.AssistantMessageRole
import com.sstt.data.assistant.model.ChatMessageRecord
import com.sstt.data.assistant.model.ChatThreadRecord
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AssistantReadRepository(private val databaseClient: DatabaseClient) {
    fun findThreadById(threadId: UUID): Mono<ChatThreadRecord> {
        return databaseClient.sql(
            """
            select thread_id, student_id, title, last_message_at, stream_version, created_at, updated_at
            from projections.chat_threads
            where thread_id = :thread_id
            """.trimIndent(),
        )
            .bind("thread_id", threadId)
            .map { row -> mapThread(row) }
            .one()
    }

    fun listThreads(studentId: UUID): Flux<ChatThreadRecord> {
        return databaseClient.sql(
            """
            select thread_id, student_id, title, last_message_at, stream_version, created_at, updated_at
            from projections.chat_threads
            where student_id = :student_id
            order by coalesce(last_message_at, created_at) desc
            """.trimIndent(),
        )
            .bind("student_id", studentId)
            .map { row -> mapThread(row) }
            .all()
    }

    fun listMessages(threadId: UUID): Flux<ChatMessageRecord> {
        return databaseClient.sql(
            """
            select message_id, thread_id, role, content, created_at
            from projections.chat_messages
            where thread_id = :thread_id
            order by created_at, message_id
            """.trimIndent(),
        )
            .bind("thread_id", threadId)
            .map { row ->
                ChatMessageRecord(
                    messageId = row.require("message_id", UUID::class.java),
                    threadId = row.require("thread_id", UUID::class.java),
                    role = AssistantMessageRole.valueOf(row.require("role", String::class.java)),
                    content = row.require("content", String::class.java),
                    createdAt = row.require("created_at", Instant::class.java),
                )
            }
            .all()
    }

    private fun mapThread(row: io.r2dbc.spi.Readable): ChatThreadRecord {
        return ChatThreadRecord(
            threadId = row.require("thread_id", UUID::class.java),
            studentId = row.require("student_id", UUID::class.java),
            title = row.require("title", String::class.java),
            lastMessageAt = row.get("last_message_at", Instant::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
