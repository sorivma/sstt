package com.sstt.app.web.identity

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import reactor.core.publisher.Mono
import jakarta.validation.Valid

/**
 * Server-rendered profile page for viewing and editing the current student
 * card.
 */
@Controller
class ProfilePageController(
    private val profileService: CurrentStudentProfileService,
) {
    @GetMapping("/", "/profile")
    fun profile(model: Model): Mono<String> {
        return profileService.currentProfile()
            .map { student ->
                model.addAttribute(
                    "profileForm",
                    ProfileForm(
                        fullName = student.fullName,
                        email = student.email,
                        groupName = student.groupName,
                    ),
                )
                model.addAttribute("student", student)
                model.addAttribute("saved", false)
                "profile"
            }
    }

    @PostMapping("/profile")
    fun updateProfile(
        @Valid @ModelAttribute("profileForm") form: ProfileForm,
        bindingResult: BindingResult,
        model: Model,
    ): Mono<String> {
        if (bindingResult.hasErrors()) {
            model.addAttribute("saved", false)
            return Mono.just("profile")
        }

        return profileService.updateProfile(form)
            .map { student ->
                model.addAttribute("student", student)
                model.addAttribute(
                    "profileForm",
                    ProfileForm(
                        fullName = student.fullName,
                        email = student.email,
                        groupName = student.groupName,
                    ),
                )
                model.addAttribute("saved", true)
                "profile"
            }
    }
}
