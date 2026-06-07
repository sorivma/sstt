package com.sstt.data.identity

import com.sstt.data.identity.model.ChangeStudentProfileCommand
import com.sstt.data.identity.model.RegisterStudentCommand
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for identity command validation.
 */
class IdentityModelTest {
    @Test
    fun `register command requires non-blank profile fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            RegisterStudentCommand(uuid(1), uuid(2), "", "student@example.com", "IKBO-01-23")
        }
    }

    @Test
    fun `change profile command requires positive expected stream version`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChangeStudentProfileCommand(uuid(1), uuid(2), 0, "Ivan", "student@example.com", "IKBO-01-23")
        }
    }

    private fun uuid(value: Long): UUID {
        return UUID.fromString("00000000-0000-0000-0000-${value.toString(16).padStart(12, '0')}")
    }
}
