package com.sstt.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentTask(
        UUID id,
        UUID userId,
        UUID subjectId,
        String subjectName,
        UUID teacherId,
        String teacherName,
        UUID sourceMessageId,
        String title,
        String description,
        TaskType type,
        TaskStatus status,
        TaskPriority priority,
        LocalDate deadlineDate,
        Double confidence,
        Instant createdAt,
        Instant updatedAt
) {
}
