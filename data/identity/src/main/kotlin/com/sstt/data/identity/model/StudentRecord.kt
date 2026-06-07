package com.sstt.data.identity.model

import java.time.Instant
import java.util.UUID

/**
 * Read model row for the current student profile projection.
 *
 * This type is not the source of truth. It mirrors the latest accepted state
 * derived from `StudentRegistered` and `StudentProfileChanged` events so UI and
 * application services can query identity data without replaying streams.
 */
data class StudentRecord(
    val studentId: UUID,
    val fullName: String,
    val email: String,
    val groupName: String,
    val streamVersion: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
