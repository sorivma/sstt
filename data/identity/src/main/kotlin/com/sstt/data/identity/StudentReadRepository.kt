package com.sstt.data.identity

import com.sstt.data.identity.model.StudentRecord
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Reactive reader for the `projections.students` projection table.
 *
 * Consumers use this repository for current student profile queries. The table
 * is rebuildable from the event log, so this repository deliberately exposes
 * read-only operations.
 */
class StudentReadRepository(
    private val databaseClient: DatabaseClient,
) {
    fun findById(studentId: UUID): Mono<StudentRecord> {
        return databaseClient.sql(
            """
            select student_id, full_name, email, group_name, stream_version, created_at, updated_at
            from projections.students
            where student_id = :student_id
            """.trimIndent(),
        )
            .bind("student_id", studentId)
            .map { row -> mapStudent(row) }
            .one()
    }

    fun findAll(): Flux<StudentRecord> {
        return databaseClient.sql(
            """
            select student_id, full_name, email, group_name, stream_version, created_at, updated_at
            from projections.students
            order by full_name, student_id
            """.trimIndent(),
        )
            .map { row -> mapStudent(row) }
            .all()
    }

    private fun mapStudent(row: io.r2dbc.spi.Readable): StudentRecord {
        return StudentRecord(
            studentId = row.require("student_id", UUID::class.java),
            fullName = row.require("full_name", String::class.java),
            email = row.require("email", String::class.java),
            groupName = row.require("group_name", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
