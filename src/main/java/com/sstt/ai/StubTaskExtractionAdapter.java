package com.sstt.ai;

import com.sstt.sources.SourceMessage;
import com.sstt.tasks.TaskPriority;
import com.sstt.tasks.TaskType;
import org.springframework.stereotype.Component;

@Component
public class StubTaskExtractionAdapter implements TaskExtractionPort {
    @Override
    public TaskExtractionResult extract(SourceMessage sourceMessage) {
        String normalized = sourceMessage.rawText().strip();
        String firstLine = normalized.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("Новая учебная задача");
        String title = firstLine.length() > 90 ? firstLine.substring(0, 87) + "..." : firstLine;
        return new TaskExtractionResult(title, normalized, TaskType.OTHER, TaskPriority.MEDIUM, null, 0.25);
    }
}
