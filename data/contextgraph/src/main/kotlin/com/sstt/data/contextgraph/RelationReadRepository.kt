package com.sstt.data.contextgraph

import com.sstt.data.contextgraph.model.RelationRecord
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RelationReadRepository(private val databaseClient: DatabaseClient) {
    fun findById(relationId: UUID): Mono<RelationRecord> {
        return selectBase("where relation_id = :relation_id")
            .bind("relation_id", relationId)
            .map { row -> mapRecord(row) }
            .one()
    }

    fun listActiveForEntity(studentId: UUID, entityType: String, entityId: UUID): Flux<RelationRecord> {
        return selectBase(
            """
            where student_id = :student_id
              and active = true
              and ((from_entity_type = :entity_type and from_entity_id = :entity_id)
                or (to_entity_type = :entity_type and to_entity_id = :entity_id))
            order by updated_at desc
            """.trimIndent(),
        )
            .bind("student_id", studentId)
            .bind("entity_type", entityType)
            .bind("entity_id", entityId)
            .map { row -> mapRecord(row) }
            .all()
    }

    private fun selectBase(whereClause: String): DatabaseClient.GenericExecuteSpec {
        return databaseClient.sql(
            """
            select relation_id, student_id, from_entity_type, from_entity_id, relation_type,
                   to_entity_type, to_entity_id, active, stream_version, created_at, updated_at
            from projections.relations
            $whereClause
            """.trimIndent(),
        )
    }

    private fun mapRecord(row: io.r2dbc.spi.Readable): RelationRecord {
        return RelationRecord(
            relationId = row.require("relation_id", UUID::class.java),
            studentId = row.require("student_id", UUID::class.java),
            fromEntityType = row.require("from_entity_type", String::class.java),
            fromEntityId = row.require("from_entity_id", UUID::class.java),
            relationType = row.require("relation_type", String::class.java),
            toEntityType = row.require("to_entity_type", String::class.java),
            toEntityId = row.require("to_entity_id", UUID::class.java),
            active = row.require("active", java.lang.Boolean::class.java).booleanValue(),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
