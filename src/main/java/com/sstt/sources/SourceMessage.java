package com.sstt.sources;

import java.time.Instant;
import java.util.UUID;

public record SourceMessage(
        UUID id,
        UUID userId,
        SourceType sourceType,
        String rawText,
        String sender,
        Instant receivedAt,
        ProcessingStatus processingStatus,
        Instant createdAt
) {
}
