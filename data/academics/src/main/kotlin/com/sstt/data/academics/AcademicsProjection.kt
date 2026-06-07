package com.sstt.data.academics

import com.sstt.data.academics.events.AcademicsEventTypes
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import java.time.LocalDate
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class AcademicsProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            AcademicsEventTypes.SUBJECT_CREATED -> upsertSubject(event, includeCreatedAt = true)
            AcademicsEventTypes.SUBJECT_RENAMED -> upsertSubject(event, includeCreatedAt = false)
            AcademicsEventTypes.TEACHER_CREATED -> upsertTeacher(event, includeCreatedAt = true)
            AcademicsEventTypes.TEACHER_PROFILE_CHANGED -> upsertTeacher(event, includeCreatedAt = false)
            AcademicsEventTypes.SEMESTER_CREATED -> upsertSemester(event)
            else -> Mono.empty()
        }
    }

    private fun upsertSubject(event: StoredEvent, includeCreatedAt: Boolean): Mono<Void> {
        val sql = if (includeCreatedAt) {
            """
            insert into projections.subjects (subject_id, name, stream_version, created_at, updated_at)
            values (:subject_id, :name, :stream_version, :created_at, :updated_at)
            on conflict (subject_id) do update
            set name = excluded.name, stream_version = excluded.stream_version, updated_at = excluded.updated_at
            where projections.subjects.stream_version < excluded.stream_version
            """.trimIndent()
        } else {
            """
            update projections.subjects
            set name = :name, stream_version = :stream_version, updated_at = :updated_at
            where subject_id = :subject_id and stream_version < :stream_version
            """.trimIndent()
        }
        var spec = databaseClient.sql(sql)
            .bind("subject_id", UUID.fromString(event.payload["subject_id"].asText()))
            .bind("name", event.payload["name"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
        if (includeCreatedAt) spec = spec.bind("created_at", event.occurredAt)
        return spec.fetch().rowsUpdated().then()
    }

    private fun upsertTeacher(event: StoredEvent, includeCreatedAt: Boolean): Mono<Void> {
        val sql = if (includeCreatedAt) {
            """
            insert into projections.teachers (teacher_id, full_name, email, stream_version, created_at, updated_at)
            values (:teacher_id, :full_name, :email, :stream_version, :created_at, :updated_at)
            on conflict (teacher_id) do update
            set full_name = excluded.full_name, email = excluded.email, stream_version = excluded.stream_version, updated_at = excluded.updated_at
            where projections.teachers.stream_version < excluded.stream_version
            """.trimIndent()
        } else {
            """
            update projections.teachers
            set full_name = :full_name, email = :email, stream_version = :stream_version, updated_at = :updated_at
            where teacher_id = :teacher_id and stream_version < :stream_version
            """.trimIndent()
        }
        var spec = databaseClient.sql(sql)
            .bind("teacher_id", UUID.fromString(event.payload["teacher_id"].asText()))
            .bind("full_name", event.payload["full_name"].asText())
            .bindNullableText("email", event.payload["email"]?.takeUnless { it.isNull }?.asText())
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
        if (includeCreatedAt) spec = spec.bind("created_at", event.occurredAt)
        return spec.fetch().rowsUpdated().then()
    }

    private fun upsertSemester(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into projections.semesters (semester_id, name, starts_on, ends_on, stream_version, created_at, updated_at)
            values (:semester_id, :name, :starts_on, :ends_on, :stream_version, :created_at, :updated_at)
            on conflict (semester_id) do update
            set name = excluded.name, starts_on = excluded.starts_on, ends_on = excluded.ends_on,
                stream_version = excluded.stream_version, updated_at = excluded.updated_at
            where projections.semesters.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("semester_id", UUID.fromString(event.payload["semester_id"].asText()))
            .bind("name", event.payload["name"].asText())
            .bindNullableDate("starts_on", event.payload["starts_on"]?.takeUnless { it.isNull }?.asText())
            .bindNullableDate("ends_on", event.payload["ends_on"]?.takeUnless { it.isNull }?.asText())
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun DatabaseClient.GenericExecuteSpec.bindNullableText(name: String, value: String?): DatabaseClient.GenericExecuteSpec {
        return if (value == null) bindNull(name, String::class.java) else bind(name, value)
    }

    private fun DatabaseClient.GenericExecuteSpec.bindNullableDate(name: String, value: String?): DatabaseClient.GenericExecuteSpec {
        return if (value == null) bindNull(name, LocalDate::class.java) else bind(name, LocalDate.parse(value))
    }

    companion object {
        const val PROJECTION_NAME = "projections.academics_catalog"
    }
}
