package com.sstt.data.tasks.model

import java.time.Instant
import java.util.UUID

enum class TaskStatus {
    BACKLOG,
    TODO,
    IN_PROGRESS,
    WAITING,
    DONE,
}

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
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
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val deadline: Instant? = null,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
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
    val status: TaskStatus,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
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
    val status: TaskStatus,
    val priority: TaskPriority,
    val deadline: Instant?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
