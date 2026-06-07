package com.sstt.data.assistant.model

import java.time.Instant
import java.util.UUID

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class StartChatThreadCommand(
    val threadId: UUID,
    val eventId: UUID,
    val studentId: UUID,
    val title: String,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
    }
}

data class AppendChatMessageCommand(
    val threadId: UUID,
    val messageId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val role: AssistantMessageRole,
    val content: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(content.isNotBlank()) { "content must not be blank" }
    }
}

data class ChatThreadRecord(
    val threadId: UUID,
    val studentId: UUID,
    val title: String,
    val lastMessageAt: Instant?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ChatMessageRecord(
    val messageId: UUID,
    val threadId: UUID,
    val role: AssistantMessageRole,
    val content: String,
    val createdAt: Instant,
)
