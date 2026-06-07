package com.sstt.data.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import com.sstt.data.tasks.events.TaskEventTypes
import com.sstt.data.tasks.model.ChangeTaskDetailsCommand
import com.sstt.data.tasks.model.ChangeTaskStatusCommand
import com.sstt.data.tasks.model.CreateTaskCommand
import reactor.core.publisher.Mono
import java.util.UUID

class TaskEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun create(command: CreateTaskCommand): Mono<AppendEventsResult> {
        return append(
            streamName = streamName(command.taskId),
            expectedVersion = ExpectedVersion.NoStream,
            eventId = command.eventId,
            eventType = TaskEventTypes.TASK_CREATED,
            payload = mapOf(
                "task_id" to command.taskId,
                "student_id" to command.studentId,
                "title" to command.title,
                "description" to command.description,
                "subject_id" to command.subjectId,
                "teacher_id" to command.teacherId,
                "semester_id" to command.semesterId,
                "status" to command.status.name,
                "priority" to command.priority.name,
                "deadline" to command.deadline,
            ),
        )
    }

    fun changeDetails(command: ChangeTaskDetailsCommand): Mono<AppendEventsResult> {
        return append(
            streamName = streamName(command.taskId),
            expectedVersion = ExpectedVersion.Exact(command.expectedStreamVersion),
            eventId = command.eventId,
            eventType = TaskEventTypes.TASK_DETAILS_CHANGED,
            payload = mapOf(
                "task_id" to command.taskId,
                "title" to command.title,
                "description" to command.description,
                "subject_id" to command.subjectId,
                "teacher_id" to command.teacherId,
                "semester_id" to command.semesterId,
                "priority" to command.priority.name,
                "deadline" to command.deadline,
            ),
        )
    }

    fun changeStatus(command: ChangeTaskStatusCommand): Mono<AppendEventsResult> {
        return append(
            streamName = streamName(command.taskId),
            expectedVersion = ExpectedVersion.Exact(command.expectedStreamVersion),
            eventId = command.eventId,
            eventType = TaskEventTypes.TASK_STATUS_CHANGED,
            payload = mapOf("task_id" to command.taskId, "status" to command.status.name),
        )
    }

    private fun append(
        streamName: String,
        expectedVersion: ExpectedVersion,
        eventId: UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName,
                streamType = STREAM_TYPE,
                expectedVersion = expectedVersion,
                events = listOf(
                    NewEvent(
                        eventId = eventId,
                        eventType = eventType,
                        eventVersion = 1,
                        payload = objectMapper.valueToTree(payload),
                        metadata = objectMapper.createObjectNode(),
                    ),
                ),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "task"

        fun streamName(taskId: Any): String = "task:$taskId"
    }
}
