package com.sstt.sources;

import com.sstt.common.DemoUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SourceMessageController {
    private final SourceMessageRepository sourceMessageRepository;

    public SourceMessageController(SourceMessageRepository sourceMessageRepository) {
        this.sourceMessageRepository = sourceMessageRepository;
    }

    @GetMapping("/sources")
    public String sources(Model model) {
        model.addAttribute("messages", sourceMessageRepository.findRecentByUserId(DemoUser.ID));
        return "sources/list";
    }

    @GetMapping("/sources/new")
    public String newSource(Model model) {
        model.addAttribute("sourceMessageForm", new SourceMessageForm());
        model.addAttribute("sourceTypes", SourceType.values());
        return "sources/form";
    }

    @PostMapping("/sources")
    public String createSource(@Valid @ModelAttribute SourceMessageForm sourceMessageForm,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("sourceTypes", SourceType.values());
            return "sources/form";
        }
        UUID sourceMessageId = sourceMessageRepository.create(DemoUser.ID, sourceMessageForm);
        return "redirect:/sources/" + sourceMessageId + "/draft-task";
    }
}
