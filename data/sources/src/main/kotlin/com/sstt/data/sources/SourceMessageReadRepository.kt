package com.sstt.data.sources

import com.sstt.data.sources.model.SourceMessageRecord
import com.sstt.data.sources.model.SourceMessageStatus
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class SourceMessageReadRepository(private val databaseClient: DatabaseClient) {
    fun findById(messageId: UUID): Mono<SourceMessageRecord> {
        return selectBase("where message_id = :message_id")
            .bind("message_id", messageId)
            .map { row -> mapRecord(row) }
            .one()
    }

    fun listByStudent(studentId: UUID): Flux<SourceMessageRecord> {
        return selectBase("where student_id = :student_id order by created_at desc")
            .bind("student_id", studentId)
            .map { row -> mapRecord(row) }
            .all()
    }

    private fun selectBase(whereClause: String): DatabaseClient.GenericExecuteSpec {
        return databaseClient.sql(
            """
            select message_id, student_id, source_type, content, status, processing_summary,
                   failure_reason, stream_version, created_at, updated_at
            from projections.source_messages
            $whereClause
            """.trimIndent(),
        )
    }

    private fun mapRecord(row: io.r2dbc.spi.Readable): SourceMessageRecord {
        return SourceMessageRecord(
            messageId = row.require("message_id", UUID::class.java),
            studentId = row.require("student_id", UUID::class.java),
            sourceType = row.require("source_type", String::class.java),
            content = row.require("content", String::class.java),
            status = SourceMessageStatus.valueOf(row.require("status", String::class.java)),
            processingSummary = row.get("processing_summary", String::class.java),
            failureReason = row.get("failure_reason", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
