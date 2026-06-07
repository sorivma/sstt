package com.sstt.tasks;

import com.sstt.ai.TaskExtractionPort;
import com.sstt.ai.TaskExtractionResult;
import com.sstt.sources.SourceMessage;
import org.springframework.stereotype.Service;

@Service
public class TaskDraftService {
    private final TaskExtractionPort taskExtractionPort;

    public TaskDraftService(TaskExtractionPort taskExtractionPort) {
        this.taskExtractionPort = taskExtractionPort;
    }

    public TaskForm createDraft(SourceMessage sourceMessage) {
        TaskExtractionResult result = taskExtractionPort.extract(sourceMessage);
        TaskForm form = new TaskForm();
        form.setTitle(result.title());
        form.setDescription(result.description());
        form.setType(result.type());
        form.setPriority(result.priority());
        form.setDeadlineDate(result.deadlineDate());
        form.setConfidence(result.confidence());
        form.setSourceMessageId(sourceMessage.id());
        return form;
    }
}
