package com.sstt.data.contextgraph

import com.fasterxml.jackson.databind.JsonNode
import com.sstt.data.contextgraph.events.ContextGraphEventTypes
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class RelationProjection(private val databaseClient: DatabaseClient) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            ContextGraphEventTypes.RELATION_ADDED -> applyAdded(event)
            ContextGraphEventTypes.RELATION_REMOVED -> applyRemoved(event)
            else -> Mono.empty()
        }
    }

    private fun applyAdded(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into projections.relations (
                relation_id, student_id, from_entity_type, from_entity_id, relation_type,
                to_entity_type, to_entity_id, active, stream_version, created_at, updated_at
            )
            values (
                :relation_id, :student_id, :from_entity_type, :from_entity_id, :relation_type,
                :to_entity_type, :to_entity_id, true, :stream_version, :created_at, :updated_at
            )
            on conflict (relation_id) do update
            set active = true,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where projections.relations.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bind("relation_id", uuid(event.payload["relation_id"]))
            .bind("student_id", uuid(event.payload["student_id"]))
            .bind("from_entity_type", event.payload["from_entity_type"].asText())
            .bind("from_entity_id", uuid(event.payload["from_entity_id"]))
            .bind("relation_type", event.payload["relation_type"].asText())
            .bind("to_entity_type", event.payload["to_entity_type"].asText())
            .bind("to_entity_id", uuid(event.payload["to_entity_id"]))
            .bind("stream_version", event.streamVersion)
            .bind("created_at", event.occurredAt)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyRemoved(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            update projections.relations
            set active = false, stream_version = :stream_version, updated_at = :updated_at
            where relation_id = :relation_id and stream_version < :stream_version
            """.trimIndent(),
        )
            .bind("relation_id", uuid(event.payload["relation_id"]))
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun uuid(node: JsonNode): UUID = UUID.fromString(node.asText())

    companion object {
        const val PROJECTION_NAME = "projections.relations"
    }
}
