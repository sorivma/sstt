package com.sstt.data.assistant

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.assistant.events.AssistantEventTypes
import com.sstt.data.assistant.model.AppendChatMessageCommand
import com.sstt.data.assistant.model.StartChatThreadCommand
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import java.util.UUID
import reactor.core.publisher.Mono

class AssistantEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun startThread(command: StartChatThreadCommand): Mono<AppendEventsResult> {
        return append(
            command.threadId,
            ExpectedVersion.NoStream,
            command.eventId,
            AssistantEventTypes.CHAT_THREAD_STARTED,
            mapOf("thread_id" to command.threadId, "student_id" to command.studentId, "title" to command.title),
        )
    }

    fun appendMessage(command: AppendChatMessageCommand): Mono<AppendEventsResult> {
        return append(
            command.threadId,
            ExpectedVersion.Exact(command.expectedStreamVersion),
            command.eventId,
            AssistantEventTypes.CHAT_MESSAGE_APPENDED,
            mapOf(
                "thread_id" to command.threadId,
                "message_id" to command.messageId,
                "role" to command.role.name,
                "content" to command.content,
            ),
        )
    }

    private fun append(
        threadId: UUID,
        expectedVersion: ExpectedVersion,
        eventId: UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(threadId),
                streamType = STREAM_TYPE,
                expectedVersion = expectedVersion,
                events = listOf(NewEvent(eventId, eventType, 1, objectMapper.valueToTree(payload), objectMapper.createObjectNode())),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "assistant-chat-thread"

        fun streamName(threadId: Any): String = "assistant-chat-thread:$threadId"
    }
}
