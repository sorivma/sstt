package com.sstt.app.web.tasks

import jakarta.validation.Valid
import java.util.UUID
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import reactor.core.publisher.Mono

@Controller
class TasksPageController(
    private val taskBoardService: TaskBoardService,
) {
    @GetMapping("/tasks")
    fun board(model: Model): Mono<String> {
        return taskBoardService.board()
            .map { board ->
                model.addAttribute("columns", board.columns)
                model.addAttribute("metrics", board.metrics)
                model.addAttribute("taskForm", CreateTaskForm())
                model.addAttribute("statusForm", CreateTaskStatusForm())
                model.addAttribute("priorities", listOf("LOW", "NORMAL", "HIGH", "URGENT"))
                "tasks/board"
            }
    }

    @PostMapping("/tasks")
    fun createTask(@Valid @ModelAttribute("taskForm") form: CreateTaskForm): Mono<String> {
        return taskBoardService.createTask(form).thenReturn("redirect:/tasks")
    }

    @PostMapping("/tasks/{taskId}/status")
    fun moveTask(
        @PathVariable taskId: UUID,
        @Valid @ModelAttribute form: MoveTaskForm,
    ): Mono<String> {
        return taskBoardService.moveTask(taskId, form.statusKey).thenReturn("redirect:/tasks")
    }

    @PostMapping("/tasks/workflow/statuses")
    fun createStatus(@Valid @ModelAttribute("statusForm") form: CreateTaskStatusForm): Mono<String> {
        return taskBoardService.createStatus(form).thenReturn("redirect:/tasks")
    }

    @PostMapping("/tasks/workflow/statuses/{statusId}")
    fun renameStatus(
        @PathVariable statusId: UUID,
        @Valid @ModelAttribute form: RenameTaskStatusForm,
    ): Mono<String> {
        return taskBoardService.renameStatus(statusId, form).thenReturn("redirect:/tasks")
    }

    @PostMapping("/tasks/workflow/statuses/{statusId}/order")
    fun reorderStatus(
        @PathVariable statusId: UUID,
        @Valid @ModelAttribute form: ReorderTaskStatusForm,
    ): Mono<String> {
        return taskBoardService.reorderStatus(statusId, form.direction).thenReturn("redirect:/tasks")
    }

    @PostMapping("/tasks/workflow/statuses/{statusId}/disable")
    fun disableStatus(@PathVariable statusId: UUID): Mono<String> {
        return taskBoardService.disableStatus(statusId).thenReturn("redirect:/tasks")
    }
}

data class TaskBoardColumn(
    val key: String,
    val title: String,
    val icon: String,
    val statusId: UUID,
    val streamVersion: Long,
    val tasks: List<TaskBoardCard> = emptyList(),
)

data class TaskBoardCard(
    val id: UUID,
    val streamVersion: Long,
    val title: String,
    val description: String?,
    val deadline: String?,
    val priority: String?,
)
