package com.sstt.app.web.identity

import com.sstt.data.identity.model.StudentRecord
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * REST API for the current student profile.
 */
@RestController
@RequestMapping("/api/me")
class ProfileRestController(
    private val profileService: CurrentStudentProfileService,
) {
    @GetMapping
    fun getMe(): Mono<StudentRecord> {
        return profileService.currentProfile()
    }

    @PutMapping
    fun updateMe(@Valid @RequestBody form: ProfileForm): Mono<StudentRecord> {
        return profileService.updateProfile(form)
    }
}
