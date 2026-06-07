package com.sstt.data.identity.model

import java.util.UUID

/**
 * Command data for changing the current profile fields of an existing student.
 *
 * [expectedStreamVersion] must be the version observed by the command handler
 * when it reconstructed the student stream. This preserves event-sourced
 * optimistic concurrency for profile edits.
 */
data class ChangeStudentProfileCommand(
    val studentId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val fullName: String,
    val email: String,
    val groupName: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(fullName.isNotBlank()) { "fullName must not be blank" }
        require(email.isNotBlank()) { "email must not be blank" }
        require(groupName.isNotBlank()) { "groupName must not be blank" }
    }
}
