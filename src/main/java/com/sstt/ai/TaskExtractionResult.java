package com.sstt.ai;

import com.sstt.tasks.TaskPriority;
import com.sstt.tasks.TaskType;
import java.time.LocalDate;

public record TaskExtractionResult(
        String title,
        String description,
        TaskType type,
        TaskPriority priority,
        LocalDate deadlineDate,
        Double confidence
) {
}
