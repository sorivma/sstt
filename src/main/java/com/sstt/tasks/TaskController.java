package com.sstt.tasks;

import com.sstt.academics.SubjectRepository;
import com.sstt.academics.TeacherRepository;
import com.sstt.common.DemoUser;
import com.sstt.sources.SourceMessage;
import com.sstt.sources.SourceMessageRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TaskController {
    private final TaskRepository taskRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SourceMessageRepository sourceMessageRepository;
    private final TaskDraftService taskDraftService;

    public TaskController(
            TaskRepository taskRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            SourceMessageRepository sourceMessageRepository,
            TaskDraftService taskDraftService
    ) {
        this.taskRepository = taskRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.sourceMessageRepository = sourceMessageRepository;
        this.taskDraftService = taskDraftService;
    }

    @GetMapping("/tasks")
    public String tasks(Model model) {
        model.addAttribute("tasks", taskRepository.findAllByUserId(DemoUser.ID));
        return "tasks/list";
    }

    @GetMapping("/tasks/new")
    public String newTask(Model model) {
        prepareTaskForm(model, new TaskForm(), null);
        return "tasks/form";
    }

    @GetMapping("/sources/{sourceMessageId}/draft-task")
    public String draftTask(@PathVariable UUID sourceMessageId, Model model) {
        SourceMessage sourceMessage = sourceMessageRepository.findByIdAndUserId(sourceMessageId, DemoUser.ID)
                .orElseThrow(() -> new IllegalArgumentException("Source message not found"));
        prepareTaskForm(model, taskDraftService.createDraft(sourceMessage), sourceMessage);
        return "tasks/form";
    }

    @PostMapping("/tasks")
    public String createTask(@Valid @ModelAttribute TaskForm taskForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareTaskForm(model, taskForm, null);
            return "tasks/form";
        }
        taskRepository.create(DemoUser.ID, taskForm);
        if (taskForm.getSourceMessageId() != null) {
            sourceMessageRepository.markProcessed(taskForm.getSourceMessageId(), DemoUser.ID);
        }
        return "redirect:/tasks";
    }

    private void prepareTaskForm(Model model, TaskForm taskForm, SourceMessage sourceMessage) {
        model.addAttribute("taskForm", taskForm);
        model.addAttribute("sourceMessage", sourceMessage);
        model.addAttribute("subjects", subjectRepository.findAllByUserId(DemoUser.ID));
        model.addAttribute("teachers", teacherRepository.findAllByUserId(DemoUser.ID));
        model.addAttribute("types", TaskType.values());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
    }
}
