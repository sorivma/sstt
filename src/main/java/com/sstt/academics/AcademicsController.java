package com.sstt.academics;

import com.sstt.common.DemoUser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AcademicsController {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public AcademicsController(SubjectRepository subjectRepository, TeacherRepository teacherRepository) {
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping("/subjects")
    public String subjects(Model model) {
        model.addAttribute("subjects", subjectRepository.findAllByUserId(DemoUser.ID));
        model.addAttribute("subjectForm", new SubjectForm());
        return "academics/subjects";
    }

    @PostMapping("/subjects")
    public String createSubject(@Valid @ModelAttribute SubjectForm subjectForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("subjects", subjectRepository.findAllByUserId(DemoUser.ID));
            return "academics/subjects";
        }
        subjectRepository.create(DemoUser.ID, subjectForm);
        return "redirect:/subjects";
    }

    @GetMapping("/teachers")
    public String teachers(Model model) {
        model.addAttribute("teachers", teacherRepository.findAllByUserId(DemoUser.ID));
        model.addAttribute("teacherForm", new TeacherForm());
        return "academics/teachers";
    }

    @PostMapping("/teachers")
    public String createTeacher(@Valid @ModelAttribute TeacherForm teacherForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("teachers", teacherRepository.findAllByUserId(DemoUser.ID));
            return "academics/teachers";
        }
        teacherRepository.create(DemoUser.ID, teacherForm);
        return "redirect:/teachers";
    }
}
