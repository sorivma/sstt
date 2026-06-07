package com.sstt.academics;

import java.time.Instant;
import java.util.UUID;

public record Subject(
        UUID id,
        UUID userId,
        String name,
        String description,
        String semester,
        Instant createdAt
) {
}
