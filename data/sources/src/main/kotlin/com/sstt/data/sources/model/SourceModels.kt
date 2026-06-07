package com.sstt.data.sources.model

import java.time.Instant
import java.util.UUID

enum class SourceMessageStatus {
    NEW,
    PROCESSED,
    FAILED,
}

data class AddSourceMessageCommand(
    val messageId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val sourceType: String,
    val content: String,
) {
    init {
        require(sourceType.isNotBlank()) { "sourceType must not be blank" }
        require(content.isNotBlank()) { "content must not be blank" }
    }
}

data class MarkSourceMessageProcessedCommand(
    val messageId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val processingSummary: String? = null,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
    }
}

data class MarkSourceMessageFailedCommand(
    val messageId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val failureReason: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(failureReason.isNotBlank()) { "failureReason must not be blank" }
    }
}

data class SourceMessageRecord(
    val messageId: UUID,
    val studentId: UUID,
    val sourceType: String,
    val content: String,
    val status: SourceMessageStatus,
    val processingSummary: String?,
    val failureReason: String?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
