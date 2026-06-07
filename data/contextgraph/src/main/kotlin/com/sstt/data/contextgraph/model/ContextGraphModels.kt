package com.sstt.data.contextgraph.model

import java.time.Instant
import java.util.UUID

data class AddRelationCommand(
    val relationId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val fromEntityType: String,
    val fromEntityId: UUID,
    val relationType: String,
    val toEntityType: String,
    val toEntityId: UUID,
) {
    init {
        require(fromEntityType.isNotBlank()) { "fromEntityType must not be blank" }
        require(relationType.isNotBlank()) { "relationType must not be blank" }
        require(toEntityType.isNotBlank()) { "toEntityType must not be blank" }
    }
}

data class RemoveRelationCommand(
    val relationId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
    }
}

data class RelationRecord(
    val relationId: UUID,
    val studentId: UUID,
    val fromEntityType: String,
    val fromEntityId: UUID,
    val relationType: String,
    val toEntityType: String,
    val toEntityId: UUID,
    val active: Boolean,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
