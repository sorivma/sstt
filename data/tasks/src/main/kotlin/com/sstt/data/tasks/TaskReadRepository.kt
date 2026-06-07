package com.sstt.data.tasks

import com.sstt.data.tasks.model.TaskPriority
import com.sstt.data.tasks.model.TaskRecord
import com.sstt.data.tasks.model.TaskStatus
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class TaskReadRepository(private val databaseClient: DatabaseClient) {
    fun findById(taskId: UUID): Mono<TaskRecord> {
        return databaseClient.sql(
            """
            select task_id, student_id, title, description, subject_id, teacher_id, semester_id,
                   status, priority, deadline, stream_version, created_at, updated_at
            from tasks.tasks
            where task_id = :task_id
            """.trimIndent(),
        )
            .bind("task_id", taskId)
            .map { row -> mapTask(row) }
            .one()
    }

    fun listByStudent(studentId: UUID): Flux<TaskRecord> {
        return databaseClient.sql(
            """
            select task_id, student_id, title, description, subject_id, teacher_id, semester_id,
                   status, priority, deadline, stream_version, created_at, updated_at
            from tasks.tasks
            where student_id = :student_id
            order by deadline nulls last, created_at desc
            """.trimIndent(),
        )
            .bind("student_id", studentId)
            .map { row -> mapTask(row) }
            .all()
    }

    fun listByStatus(studentId: UUID, status: TaskStatus): Flux<TaskRecord> {
        return databaseClient.sql(
            """
            select task_id, student_id, title, description, subject_id, teacher_id, semester_id,
                   status, priority, deadline, stream_version, created_at, updated_at
            from tasks.tasks
            where student_id = :student_id and status = :status
            order by deadline nulls last, created_at desc
            """.trimIndent(),
        )
            .bind("student_id", studentId)
            .bind("status", status.name)
            .map { row -> mapTask(row) }
            .all()
    }

    private fun mapTask(row: io.r2dbc.spi.Readable): TaskRecord {
        return TaskRecord(
            taskId = row.require("task_id", UUID::class.java),
            studentId = row.require("student_id", UUID::class.java),
            title = row.require("title", String::class.java),
            description = row.get("description", String::class.java),
            subjectId = row.get("subject_id", UUID::class.java),
            teacherId = row.get("teacher_id", UUID::class.java),
            semesterId = row.get("semester_id", UUID::class.java),
            status = TaskStatus.valueOf(row.require("status", String::class.java)),
            priority = TaskPriority.valueOf(row.require("priority", String::class.java)),
            deadline = row.get("deadline", Instant::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
