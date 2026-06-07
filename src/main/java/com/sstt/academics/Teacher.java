package com.sstt.academics;

import java.time.Instant;
import java.util.UUID;

public record Teacher(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String telegram,
        String notes,
        Instant createdAt
) {
}
