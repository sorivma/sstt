package com.sstt.data.contextgraph

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.contextgraph.events.ContextGraphEventTypes
import com.sstt.data.contextgraph.model.AddRelationCommand
import com.sstt.data.contextgraph.model.RemoveRelationCommand
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import java.util.UUID
import reactor.core.publisher.Mono

class RelationEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun add(command: AddRelationCommand): Mono<AppendEventsResult> {
        return append(
            command.relationId,
            ExpectedVersion.NoStream,
            command.eventId,
            ContextGraphEventTypes.RELATION_ADDED,
            mapOf(
                "relation_id" to command.relationId,
                "student_id" to command.studentId,
                "from_entity_type" to command.fromEntityType,
                "from_entity_id" to command.fromEntityId,
                "relation_type" to command.relationType,
                "to_entity_type" to command.toEntityType,
                "to_entity_id" to command.toEntityId,
            ),
        )
    }

    fun remove(command: RemoveRelationCommand): Mono<AppendEventsResult> {
        return append(
            command.relationId,
            ExpectedVersion.Exact(command.expectedStreamVersion),
            command.eventId,
            ContextGraphEventTypes.RELATION_REMOVED,
            mapOf("relation_id" to command.relationId),
        )
    }

    private fun append(
        relationId: UUID,
        expectedVersion: ExpectedVersion,
        eventId: UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(relationId),
                streamType = STREAM_TYPE,
                expectedVersion = expectedVersion,
                events = listOf(NewEvent(eventId, eventType, 1, objectMapper.valueToTree(payload), objectMapper.createObjectNode())),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "relation"

        fun streamName(relationId: Any): String = "relation:$relationId"
    }
}
