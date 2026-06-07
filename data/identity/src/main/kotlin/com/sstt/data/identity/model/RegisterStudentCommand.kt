package com.sstt.data.identity.model

import java.util.UUID

/**
 * Command data required to create the event stream for a student.
 *
 * The student is intentionally a small identity entity for the MVP: full name,
 * email, and academic group. [eventId] is supplied by the caller so retries can
 * safely reuse the same domain event id.
 */
data class RegisterStudentCommand(
    val studentId: UUID,
    val eventId: UUID,
    val fullName: String,
    val email: String,
    val groupName: String,
) {
    init {
        require(fullName.isNotBlank()) { "fullName must not be blank" }
        require(email.isNotBlank()) { "email must not be blank" }
        require(groupName.isNotBlank()) { "groupName must not be blank" }
    }
}
