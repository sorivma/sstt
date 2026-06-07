package com.sstt.data.materials.model

import java.time.Instant
import java.util.UUID

data class RegisterMaterialCommand(
    val materialId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val fileName: String,
    val contentType: String? = null,
    val storageKey: String,
) {
    init {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(storageKey.isNotBlank()) { "storageKey must not be blank" }
    }
}

data class ChangeMaterialSummaryCommand(
    val materialId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val summary: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(summary.isNotBlank()) { "summary must not be blank" }
    }
}

data class MaterialRecord(
    val materialId: UUID,
    val studentId: UUID,
    val fileName: String,
    val contentType: String?,
    val storageKey: String,
    val summary: String?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
