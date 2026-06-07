package com.sstt.app.web.tasks

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TasksPageController {
    @GetMapping("/tasks")
    fun board(model: Model): String {
        model.addAttribute(
            "columns",
            listOf(
                TaskBoardColumn("backlog", "Backlog", "archive"),
                TaskBoardColumn("todo", "Todo", "circle-dot"),
                TaskBoardColumn("in-progress", "In Progress", "play"),
                TaskBoardColumn("waiting", "Waiting", "clock"),
                TaskBoardColumn("done", "Done", "circle-check"),
            ),
        )
        return "tasks/board"
    }
}

data class TaskBoardColumn(
    val key: String,
    val title: String,
    val icon: String,
    val tasks: List<TaskBoardCard> = emptyList(),
)

data class TaskBoardCard(
    val title: String,
    val subject: String?,
    val teacher: String?,
    val semester: String?,
    val deadline: String?,
    val priority: String?,
)
