package com.sstt.data.academics.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateSubjectCommand(
    val subjectId: UUID,
    val eventId: UUID,
    val name: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
    }
}

data class RenameSubjectCommand(
    val subjectId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val name: String,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(name.isNotBlank()) { "name must not be blank" }
    }
}

data class CreateTeacherCommand(
    val teacherId: UUID,
    val eventId: UUID,
    val fullName: String,
    val email: String? = null,
) {
    init {
        require(fullName.isNotBlank()) { "fullName must not be blank" }
    }
}

data class ChangeTeacherProfileCommand(
    val teacherId: UUID,
    val eventId: UUID,
    val expectedStreamVersion: Long,
    val fullName: String,
    val email: String? = null,
) {
    init {
        require(expectedStreamVersion > 0) { "expectedStreamVersion must be > 0" }
        require(fullName.isNotBlank()) { "fullName must not be blank" }
    }
}

data class CreateSemesterCommand(
    val semesterId: UUID,
    val eventId: UUID,
    val name: String,
    val startsOn: LocalDate? = null,
    val endsOn: LocalDate? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(startsOn == null || endsOn == null || !endsOn.isBefore(startsOn)) {
            "endsOn must be on or after startsOn"
        }
    }
}

data class SubjectRecord(
    val subjectId: UUID,
    val name: String,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TeacherRecord(
    val teacherId: UUID,
    val fullName: String,
    val email: String?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class SemesterRecord(
    val semesterId: UUID,
    val name: String,
    val startsOn: LocalDate?,
    val endsOn: LocalDate?,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
