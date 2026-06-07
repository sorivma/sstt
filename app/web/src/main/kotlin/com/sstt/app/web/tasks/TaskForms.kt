package com.sstt.app.web.tasks

import jakarta.validation.constraints.NotBlank

data class CreateTaskForm(
    @field:NotBlank
    val title: String = "",
    val description: String = "",
    val statusKey: String = "",
    val priority: String = "NORMAL",
    val deadline: String = "",
)

data class CreateTaskStatusForm(
    @field:NotBlank
    val key: String = "",
    @field:NotBlank
    val title: String = "",
    val icon: String = "circle-dot",
)

data class RenameTaskStatusForm(
    @field:NotBlank
    val title: String = "",
    val icon: String = "circle-dot",
)

data class MoveTaskForm(
    @field:NotBlank
    val statusKey: String = "",
)

data class ReorderTaskStatusForm(
    @field:NotBlank
    val direction: String = "",
)
