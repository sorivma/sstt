package com.sstt.data.sources

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import com.sstt.data.sources.events.SourceEventTypes
import com.sstt.data.sources.model.AddSourceMessageCommand
import com.sstt.data.sources.model.MarkSourceMessageFailedCommand
import com.sstt.data.sources.model.MarkSourceMessageProcessedCommand
import java.util.UUID
import reactor.core.publisher.Mono

class SourceMessageEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun add(command: AddSourceMessageCommand): Mono<AppendEventsResult> {
        return append(
            command.messageId,
            ExpectedVersion.NoStream,
            command.eventId,
            SourceEventTypes.SOURCE_MESSAGE_ADDED,
            mapOf(
                "message_id" to command.messageId,
                "student_id" to command.studentId,
                "source_type" to command.sourceType,
                "content" to command.content,
            ),
        )
    }

    fun markProcessed(command: MarkSourceMessageProcessedCommand): Mono<AppendEventsResult> {
        return append(
            command.messageId,
            ExpectedVersion.Exact(command.expectedStreamVersion),
            command.eventId,
            SourceEventTypes.SOURCE_MESSAGE_PROCESSED,
            mapOf("message_id" to command.messageId, "processing_summary" to command.processingSummary),
        )
    }

    fun markFailed(command: MarkSourceMessageFailedCommand): Mono<AppendEventsResult> {
        return append(
            command.messageId,
            ExpectedVersion.Exact(command.expectedStreamVersion),
            command.eventId,
            SourceEventTypes.SOURCE_MESSAGE_FAILED,
            mapOf("message_id" to command.messageId, "failure_reason" to command.failureReason),
        )
    }

    private fun append(
        messageId: UUID,
        expectedVersion: ExpectedVersion,
        eventId: UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(messageId),
                streamType = STREAM_TYPE,
                expectedVersion = expectedVersion,
                events = listOf(
                    NewEvent(eventId, eventType, 1, objectMapper.valueToTree(payload), objectMapper.createObjectNode()),
                ),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "source-message"

        fun streamName(messageId: Any): String = "source-message:$messageId"
    }
}
