package com.sstt.data.academics

import com.sstt.data.academics.model.SemesterRecord
import com.sstt.data.academics.model.SubjectRecord
import com.sstt.data.academics.model.TeacherRecord
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AcademicsReadRepository(private val databaseClient: DatabaseClient) {
    fun findSubjectById(subjectId: UUID): Mono<SubjectRecord> {
        return databaseClient.sql("select subject_id, name, stream_version, created_at, updated_at from projections.subjects where subject_id = :subject_id")
            .bind("subject_id", subjectId)
            .map { row -> mapSubject(row) }
            .one()
    }

    fun findTeacherById(teacherId: UUID): Mono<TeacherRecord> {
        return databaseClient.sql("select teacher_id, full_name, email, stream_version, created_at, updated_at from projections.teachers where teacher_id = :teacher_id")
            .bind("teacher_id", teacherId)
            .map { row -> mapTeacher(row) }
            .one()
    }

    fun listSubjects(): Flux<SubjectRecord> = databaseClient.sql("select subject_id, name, stream_version, created_at, updated_at from projections.subjects order by name")
        .map { row -> mapSubject(row) }
        .all()

    fun listTeachers(): Flux<TeacherRecord> = databaseClient.sql("select teacher_id, full_name, email, stream_version, created_at, updated_at from projections.teachers order by full_name")
        .map { row -> mapTeacher(row) }
        .all()

    fun listSemesters(): Flux<SemesterRecord> = databaseClient.sql("select semester_id, name, starts_on, ends_on, stream_version, created_at, updated_at from projections.semesters order by starts_on nulls last, name")
        .map { row ->
            SemesterRecord(
                semesterId = row.require("semester_id", UUID::class.java),
                name = row.require("name", String::class.java),
                startsOn = row.get("starts_on", LocalDate::class.java),
                endsOn = row.get("ends_on", LocalDate::class.java),
                streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
                createdAt = row.require("created_at", Instant::class.java),
                updatedAt = row.require("updated_at", Instant::class.java),
            )
        }
        .all()

    private fun mapSubject(row: io.r2dbc.spi.Readable): SubjectRecord {
        return SubjectRecord(
            subjectId = row.require("subject_id", UUID::class.java),
            name = row.require("name", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun mapTeacher(row: io.r2dbc.spi.Readable): TeacherRecord {
        return TeacherRecord(
            teacherId = row.require("teacher_id", UUID::class.java),
            fullName = row.require("full_name", String::class.java),
            email = row.get("email", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
