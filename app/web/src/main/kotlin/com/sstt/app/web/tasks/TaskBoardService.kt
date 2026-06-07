package com.sstt.app.web.tasks

import com.sstt.app.web.identity.CurrentStudentProperties
import com.sstt.data.eventstore.projection.ReactiveProjectionRunner
import com.sstt.data.tasks.TaskEventWriter
import com.sstt.data.tasks.TaskProjection
import com.sstt.data.tasks.TaskReadRepository
import com.sstt.data.tasks.model.ChangeTaskStatusCommand
import com.sstt.data.tasks.model.CreateTaskCommand
import com.sstt.data.tasks.model.CreateTaskStatusCommand
import com.sstt.data.tasks.model.DisableTaskStatusCommand
import com.sstt.data.tasks.model.RenameTaskStatusCommand
import com.sstt.data.tasks.model.ReorderTaskStatusCommand
import com.sstt.data.tasks.model.TaskPriority
import com.sstt.data.tasks.model.TaskRecord
import com.sstt.data.tasks.model.TaskStatusRecord
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class TaskBoardService(
    private val properties: CurrentStudentProperties,
    private val taskEventWriter: TaskEventWriter,
    private val taskReadRepository: TaskReadRepository,
    private val projectionRunner: ReactiveProjectionRunner,
) {
    fun board(): Mono<TaskBoardView> {
        return ensureWorkflow()
            .then(loadBoard())
    }

    fun createTask(form: CreateTaskForm): Mono<Void> {
        return ensureWorkflow()
            .then(taskReadRepository.listActiveStatuses(properties.id).collectList())
            .flatMap { statuses ->
                val statusKey = form.statusKey.ifBlank { statuses.firstOrNull()?.key ?: DEFAULT_STATUS_KEY }
                taskEventWriter.create(
                    CreateTaskCommand(
                        taskId = UUID.randomUUID(),
                        eventId = UUID.randomUUID(),
                        studentId = properties.id,
                        title = form.title.trim(),
                        description = form.description.trim().ifBlank { null },
                        statusKey = statusKey,
                        priority = parsePriority(form.priority),
                        deadline = parseDeadline(form.deadline),
                    ),
                )
            }
            .then(catchUpProjection())
    }

    fun moveTask(taskId: UUID, statusKey: String): Mono<Void> {
        return taskReadRepository.findById(taskId)
            .flatMap { task ->
                taskEventWriter.changeStatus(
                    ChangeTaskStatusCommand(
                        taskId = task.taskId,
                        eventId = UUID.randomUUID(),
                        expectedStreamVersion = task.streamVersion,
                        statusKey = statusKey,
                    ),
                )
            }
            .then(catchUpProjection())
    }

    fun createStatus(form: CreateTaskStatusForm): Mono<Void> {
        return ensureWorkflow()
            .then(taskReadRepository.listActiveStatuses(properties.id).collectList())
            .flatMap { statuses ->
                val sortOrder = (statuses.maxOfOrNull { it.sortOrder } ?: 0) + SORT_STEP
                taskEventWriter.createStatus(
                    CreateTaskStatusCommand(
                        statusId = UUID.randomUUID(),
                        eventId = UUID.randomUUID(),
                        studentId = properties.id,
                        key = normalizeStatusKey(form.key),
                        title = form.title.trim(),
                        icon = form.icon.trim().ifBlank { "circle-dot" },
                        sortOrder = sortOrder,
                    ),
                )
            }
            .then(catchUpProjection())
    }

    fun renameStatus(statusId: UUID, form: RenameTaskStatusForm): Mono<Void> {
        return taskReadRepository.findStatusById(statusId)
            .flatMap { status ->
                taskEventWriter.renameStatus(
                    RenameTaskStatusCommand(
                        statusId = status.statusId,
                        eventId = UUID.randomUUID(),
                        expectedStreamVersion = status.streamVersion,
                        title = form.title.trim(),
                        icon = form.icon.trim().ifBlank { "circle-dot" },
                    ),
                )
            }
            .then(catchUpProjection())
    }

    fun reorderStatus(statusId: UUID, direction: String): Mono<Void> {
        return taskReadRepository.listActiveStatuses(properties.id).collectList()
            .flatMap { statuses ->
                val currentIndex = statuses.indexOfFirst { it.statusId == statusId }
                if (currentIndex < 0) {
                    return@flatMap Mono.empty()
                }
                val targetIndex = when (direction) {
                    "left" -> currentIndex - 1
                    "right" -> currentIndex + 1
                    else -> currentIndex
                }
                val target = statuses.getOrNull(targetIndex) ?: return@flatMap Mono.empty()
                val current = statuses[currentIndex]
                taskEventWriter.reorderStatus(
                    ReorderTaskStatusCommand(
                        statusId = current.statusId,
                        eventId = UUID.randomUUID(),
                        expectedStreamVersion = current.streamVersion,
                        sortOrder = target.sortOrder + if (direction == "left") -1 else 1,
                    ),
                )
            }
            .then(catchUpProjection())
    }

    fun disableStatus(statusId: UUID): Mono<Void> {
        return taskReadRepository.findStatusById(statusId)
            .flatMap { status ->
                taskEventWriter.disableStatus(
                    DisableTaskStatusCommand(
                        statusId = status.statusId,
                        eventId = UUID.randomUUID(),
                        expectedStreamVersion = status.streamVersion,
                    ),
                )
            }
            .then(catchUpProjection())
    }

    private fun ensureWorkflow(): Mono<Void> {
        return catchUpProjection()
            .then(taskReadRepository.listActiveStatuses(properties.id).collectList())
            .flatMapMany { statuses ->
                if (statuses.isNotEmpty()) {
                    Flux.empty()
                } else {
                    Flux.fromIterable(DEFAULT_STATUSES)
                        .concatMap { seed ->
                            taskEventWriter.createStatus(
                                CreateTaskStatusCommand(
                                    statusId = defaultStatusId(seed.key),
                                    eventId = UUID.randomUUID(),
                                    studentId = properties.id,
                                    key = seed.key,
                                    title = seed.title,
                                    icon = seed.icon,
                                    sortOrder = seed.sortOrder,
                                    terminal = seed.terminal,
                                ),
                            ).onErrorResume { Mono.empty() }
                        }
                }
            }
            .then(catchUpProjection())
    }

    private fun loadBoard(): Mono<TaskBoardView> {
        return Mono.zip(
            taskReadRepository.listActiveStatuses(properties.id).collectList(),
            taskReadRepository.listByStudent(properties.id).collectList(),
        )
            .map { tuple ->
                val statuses = tuple.t1
                val tasks = tuple.t2
                val tasksByStatus = tasks.groupBy { it.statusKey }
                TaskBoardView(
                    columns = statuses.map { status ->
                        TaskBoardColumn(
                            key = status.key,
                            title = status.title,
                            icon = status.icon,
                            statusId = status.statusId,
                            streamVersion = status.streamVersion,
                            tasks = tasksByStatus[status.key].orEmpty().map { task -> task.toCard() },
                        )
                    },
                    metrics = TaskBoardMetrics(
                        openTasks = tasks.count { task -> statuses.none { it.key == task.statusKey && it.terminal } },
                        dueWithDeadline = tasks.count { it.deadline != null },
                        configuredStatuses = statuses.size,
                        totalTasks = tasks.size,
                    ),
                )
            }
    }

    private fun TaskRecord.toCard(): TaskBoardCard {
        return TaskBoardCard(
            id = taskId,
            streamVersion = streamVersion,
            title = title,
            description = description,
            deadline = deadline?.toString(),
            priority = priority.name.lowercase(),
        )
    }

    private fun catchUpProjection(): Mono<Void> {
        return projectionRunner.runOnce(TaskProjection.PROJECTION_NAME).then()
    }

    private fun parsePriority(value: String): TaskPriority {
        return runCatching { TaskPriority.valueOf(value.uppercase()) }.getOrDefault(TaskPriority.NORMAL)
    }

    private fun parseDeadline(value: String): Instant? {
        if (value.isBlank()) return null
        return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun normalizeStatusKey(value: String): String {
        return value.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    private fun defaultStatusId(key: String): UUID {
        return UUID.nameUUIDFromBytes("task-status:${properties.id}:$key".toByteArray(StandardCharsets.UTF_8))
    }

    private data class DefaultStatus(
        val key: String,
        val title: String,
        val icon: String,
        val sortOrder: Int,
        val terminal: Boolean = false,
    )

    companion object {
        private const val SORT_STEP = 100
        private const val DEFAULT_STATUS_KEY = "todo"

        private val DEFAULT_STATUSES = listOf(
            DefaultStatus("backlog", "Backlog", "archive", 100),
            DefaultStatus("todo", "Todo", "circle-dot", 200),
            DefaultStatus("in-progress", "In Progress", "play", 300),
            DefaultStatus("waiting", "Waiting", "clock", 400),
            DefaultStatus("done", "Done", "circle-check", 500, terminal = true),
        )
    }
}

data class TaskBoardView(
    val columns: List<TaskBoardColumn>,
    val metrics: TaskBoardMetrics,
)

data class TaskBoardMetrics(
    val openTasks: Int,
    val dueWithDeadline: Int,
    val configuredStatuses: Int,
    val totalTasks: Int,
)
