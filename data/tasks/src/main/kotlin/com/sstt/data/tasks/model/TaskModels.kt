package com.sstt.data.tasks.model

import java.time.Instant
import java.util.UUID

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

data class CreateTaskStatusCommand(
    val statusId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val key: String,
    val title: String,
    val icon: String,
    val sortOrder: Int,
    val terminal: Boolean = false,
) {
    init {
        requireValidStatusKey(key)
        require(title.isNotBlank()) { "title must not be blank" }
        require(icon.isNotBlank()) { "icon must not be blank" }
    }
}

data class RenameTaskStatusCommand(
    val statusId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val title: String,
    val icon: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(icon.isNotBlank()) { "icon must not be blank" }
    }
}

data class ReorderTaskStatusCommand(
    val statusId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val sortOrder: Int,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
    }
}

data class DisableTaskStatusCommand(
    val statusId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
    }
}

data class CreateTaskCommand(
    val taskId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val title: String,
    val description: String? = null,
    val subjectId: UUID? = null,
    val teacherId: UUID? = null,
    val semesterId: UUID? = null,
    val statusKey: String = DEFAULT_TASK_STATUS_KEY,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val deadline: Instant? = null,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        requireValidStatusKey(statusKey)
    }
}

data class ChangeTaskDetailsCommand(
    val taskId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val title: String,
    val description: String? = null,
    val subjectId: UUID? = null,
    val teacherId: UUID? = null,
    val semesterId: UUID? = null,
    val priority: TaskPriority,
    val deadline: Instant? = null,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(title.isNotBlank()) { "title must not be blank" }
    }
}

data class ChangeTaskStatusCommand(
    val taskId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val statusKey: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        requireValidStatusKey(statusKey)
    }
}

data class TaskRecord(
    val taskId: UUID,
    val studentId: UUID,
    val title: String,
    val description: String?,
    val subjectId: UUID?,
    val teacherId: UUID?,
    val semesterId: UUID?,
    val statusKey: String,
    val priority: TaskPriority,
    val deadline: Instant?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TaskStatusRecord(
    val statusId: UUID,
    val studentId: UUID,
    val key: String,
    val title: String,
    val icon: String,
    val sortOrder: Int,
    val terminal: Boolean,
    val active: Boolean,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

const val DEFAULT_TASK_STATUS_KEY = "todo"

fun requireValidStatusKey(key: String) {
    require(key.matches(Regex("[a-z0-9][a-z0-9-]{0,63}"))) {
        "status key must be lower kebab-case and at most 64 characters"
    }
}
