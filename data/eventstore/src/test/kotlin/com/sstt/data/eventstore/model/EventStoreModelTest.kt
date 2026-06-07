package com.sstt.data.eventstore.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for event store public model validation.
 */
class EventStoreModelTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `new event requires non-blank type and positive version`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewEvent(uuid(1), "", 1, objectMapper.createObjectNode(), objectMapper.createObjectNode())
        }

        assertThrows(IllegalArgumentException::class.java) {
            NewEvent(uuid(1), "TaskCreated", 0, objectMapper.createObjectNode(), objectMapper.createObjectNode())
        }
    }

    @Test
    fun `append command requires stream identity and at least one event`() {
        val event = NewEvent(uuid(1), "TaskCreated", 1, objectMapper.createObjectNode(), objectMapper.createObjectNode())

        assertThrows(IllegalArgumentException::class.java) {
            AppendEventsCommand("", "task", ExpectedVersion.NoStream, listOf(event))
        }

        assertThrows(IllegalArgumentException::class.java) {
            AppendEventsCommand("task:1", "", ExpectedVersion.NoStream, listOf(event))
        }

        assertThrows(IllegalArgumentException::class.java) {
            AppendEventsCommand("task:1", "task", ExpectedVersion.NoStream, emptyList())
        }
    }

    @Test
    fun `append command requires unique event ids`() {
        val first = NewEvent(uuid(1), "TaskCreated", 1, objectMapper.createObjectNode(), objectMapper.createObjectNode())
        val second = NewEvent(uuid(1), "TaskDetailsChanged", 1, objectMapper.createObjectNode(), objectMapper.createObjectNode())

        assertThrows(IllegalArgumentException::class.java) {
            AppendEventsCommand("task:1", "task", ExpectedVersion.NoStream, listOf(first, second))
        }
    }

    @Test
    fun `exact expected version must be non-negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpectedVersion.Exact(-1)
        }
    }

    @Test
    fun `event store configuration requires positive batch size`() {
        assertThrows(IllegalArgumentException::class.java) {
            EventStoreConfiguration(defaultReadBatchSize = 0)
        }
    }

    private fun uuid(value: Long): UUID {
        return UUID.fromString("00000000-0000-0000-0000-${value.toString(16).padStart(12, '0')}")
    }
}
