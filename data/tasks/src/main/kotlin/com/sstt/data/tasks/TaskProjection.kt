package com.sstt.data.tasks

import com.fasterxml.jackson.databind.JsonNode
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import com.sstt.data.tasks.events.TaskEventTypes
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class TaskProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            TaskEventTypes.TASK_CREATED -> applyTaskCreated(event)
            TaskEventTypes.TASK_DETAILS_CHANGED -> applyDetailsChanged(event)
            TaskEventTypes.TASK_STATUS_CHANGED -> applyStatusChanged(event)
            else -> Mono.empty()
        }
    }

    private fun applyTaskCreated(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into tasks.tasks (
                task_id, student_id, title, description, subject_id, teacher_id, semester_id,
                status, priority, deadline, stream_version, created_at, updated_at
            )
            values (
                :task_id, :student_id, :title, :description, :subject_id, :teacher_id, :semester_id,
                :status, :priority, :deadline, :stream_version, :created_at, :updated_at
            )
            on conflict (task_id) do update
            set title = excluded.title,
                description = excluded.description,
                subject_id = excluded.subject_id,
                teacher_id = excluded.teacher_id,
                semester_id = excluded.semester_id,
                status = excluded.status,
                priority = excluded.priority,
                deadline = excluded.deadline,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where tasks.tasks.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("task_id", uuid(event.payload["task_id"]))
            .bind("student_id", uuid(event.payload["student_id"]))
            .bind("title", event.payload["title"].asText())
            .bindNullableText("description", optionalText(event.payload["description"]))
            .bindNullableUuid("subject_id", event.payload["subject_id"])
            .bindNullableUuid("teacher_id", event.payload["teacher_id"])
            .bindNullableUuid("semester_id", event.payload["semester_id"])
            .bind("status", event.payload["status"].asText())
            .bind("priority", event.payload["priority"].asText())
            .bindNullableInstant("deadline", event.payload["deadline"])
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyDetailsChanged(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            update tasks.tasks
            set title = :title,
                description = :description,
                subject_id = :subject_id,
                teacher_id = :teacher_id,
                semester_id = :semester_id,
                priority = :priority,
                deadline = :deadline,
                stream_version = :stream_version,
                updated_at = :updated_at
            where task_id = :task_id and stream_version < :stream_version
            """.trimIndent(),
        )
            .bind("task_id", uuid(event.payload["task_id"]))
            .bind("title", event.payload["title"].asText())
            .bindNullableText("description", optionalText(event.payload["description"]))
            .bindNullableUuid("subject_id", event.payload["subject_id"])
            .bindNullableUuid("teacher_id", event.payload["teacher_id"])
            .bindNullableUuid("semester_id", event.payload["semester_id"])
            .bind("priority", event.payload["priority"].asText())
            .bindNullableInstant("deadline", event.payload["deadline"])
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyStatusChanged(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            update tasks.tasks
            set status = :status, stream_version = :stream_version, updated_at = :updated_at
            where task_id = :task_id and stream_version < :stream_version
            """.trimIndent(),
        )
            .bind("task_id", uuid(event.payload["task_id"]))
            .bind("status", event.payload["status"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun uuid(node: JsonNode): UUID = UUID.fromString(node.asText())

    private fun optionalText(node: JsonNode?): String? = node?.takeUnless { it.isNull }?.asText()

    private fun DatabaseClient.GenericExecuteSpec.bindNullableText(name: String, value: String?): DatabaseClient.GenericExecuteSpec {
        return if (value == null) bindNull(name, String::class.java) else bind(name, value)
    }

    private fun DatabaseClient.GenericExecuteSpec.bindNullableUuid(name: String, node: JsonNode?): DatabaseClient.GenericExecuteSpec {
        return if (node == null || node.isNull) bindNull(name, UUID::class.java) else bind(name, uuid(node))
    }

    private fun DatabaseClient.GenericExecuteSpec.bindNullableInstant(name: String, node: JsonNode?): DatabaseClient.GenericExecuteSpec {
        return if (node == null || node.isNull) bindNull(name, Instant::class.java) else bind(name, Instant.parse(node.asText()))
    }

    companion object {
        const val PROJECTION_NAME = "tasks.tasks"
    }
}
