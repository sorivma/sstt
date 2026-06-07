package com.sstt.app.web.identity

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Form and REST payload for editing the current student profile.
 */
data class ProfileForm(
    @field:NotBlank
    val fullName: String = "",
    @field:NotBlank
    @field:Email
    val email: String = "",
    @field:NotBlank
    val groupName: String = "",
)
