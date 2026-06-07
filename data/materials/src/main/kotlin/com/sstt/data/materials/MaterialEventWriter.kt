package com.sstt.data.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import com.sstt.data.materials.events.MaterialEventTypes
import com.sstt.data.materials.model.ChangeMaterialSummaryCommand
import com.sstt.data.materials.model.RegisterMaterialCommand
import java.util.UUID
import reactor.core.publisher.Mono

class MaterialEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun register(command: RegisterMaterialCommand): Mono<AppendEventsResult> {
        return append(
            command.materialId,
            ExpectedVersion.NoStream,
            command.eventId,
            MaterialEventTypes.MATERIAL_REGISTERED,
            mapOf(
                "material_id" to command.materialId,
                "student_id" to command.studentId,
                "file_name" to command.fileName,
                "content_type" to command.contentType,
                "storage_key" to command.storageKey,
            ),
        )
    }

    fun changeSummary(command: ChangeMaterialSummaryCommand): Mono<AppendEventsResult> {
        return append(
            command.materialId,
            ExpectedVersion.Exact(command.expectedStreamVersion),
            command.eventId,
            MaterialEventTypes.MATERIAL_SUMMARY_CHANGED,
            mapOf("material_id" to command.materialId, "summary" to command.summary),
        )
    }

    private fun append(
        materialId: UUID,
        expectedVersion: ExpectedVersion,
        eventId: UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(materialId),
                streamType = STREAM_TYPE,
                expectedVersion = expectedVersion,
                events = listOf(NewEvent(eventId, eventType, 1, objectMapper.valueToTree(payload), objectMapper.createObjectNode())),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "material"

        fun streamName(materialId: Any): String = "material:$materialId"
    }
}
