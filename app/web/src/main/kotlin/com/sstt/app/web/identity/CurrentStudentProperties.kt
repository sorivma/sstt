package com.sstt.app.web.identity

import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Runtime identity of the MVP's current student.
 *
 * Real authentication is intentionally not introduced yet. Until the identity
 * module grows users/auth, the web app operates on one configured student
 * profile and lets the user edit that profile.
 */
@ConfigurationProperties(prefix = "sstt.current-student")
data class CurrentStudentProperties(
    val id: UUID,
    val defaultFullName: String,
    val defaultEmail: String,
    val defaultGroupName: String,
)
