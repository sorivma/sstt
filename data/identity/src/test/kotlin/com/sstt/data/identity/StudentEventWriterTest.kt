package com.sstt.data.identity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.identity.events.IdentityEventTypes
import com.sstt.data.identity.model.ChangeStudentProfileCommand
import com.sstt.data.identity.model.RegisterStudentCommand
import java.util.UUID
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Unit tests for translating student commands into generic eventstore append
 * commands.
 */
class StudentEventWriterTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `register appends student registered event to a new stream`() {
        val eventStore = CapturingEventStore()
        val writer = StudentEventWriter(eventStore, objectMapper)
        val studentId = uuid(1)
        val eventId = uuid(2)

        StepVerifier.create(
            writer.register(
                RegisterStudentCommand(
                    studentId = studentId,
                    eventId = eventId,
                    fullName = "Ivan Ivanov",
                    email = "ivan@example.com",
                    groupName = "IKBO-01-23",
                ),
            ),
        )
            .expectNextCount(1)
            .verifyComplete()

        val command = eventStore.lastCommand ?: error("append command was not captured")
        check(command.streamName == "student:$studentId")
        check(command.streamType == "student")
        check(command.expectedVersion == ExpectedVersion.NoStream)
        check(command.events.single().eventId == eventId)
        check(command.events.single().eventType == IdentityEventTypes.STUDENT_REGISTERED)
        check(command.events.single().payload["full_name"].asText() == "Ivan Ivanov")
    }

    @Test
    fun `change profile appends profile changed event with exact expected version`() {
        val eventStore = CapturingEventStore()
        val writer = StudentEventWriter(eventStore, objectMapper)
        val studentId = uuid(3)

        StepVerifier.create(
            writer.changeProfile(
                ChangeStudentProfileCommand(
                    studentId = studentId,
                    eventId = uuid(4),
                    expectedStreamVersion = 7,
                    fullName = "Ivan Petrov",
                    email = "petrov@example.com",
                    groupName = "IKBO-02-23",
                ),
            ),
        )
            .expectNextCount(1)
            .verifyComplete()

        val command = eventStore.lastCommand ?: error("append command was not captured")
        check(command.expectedVersion == ExpectedVersion.Exact(7))
        check(command.events.single().eventType == IdentityEventTypes.STUDENT_PROFILE_CHANGED)
        check(command.events.single().payload["group_name"].asText() == "IKBO-02-23")
    }

    private class CapturingEventStore : EventStore {
        var lastCommand: AppendEventsCommand? = null

        override fun append(command: AppendEventsCommand): Mono<AppendEventsResult> {
            lastCommand = command
            return Mono.just(
                AppendEventsResult(
                    streamName = command.streamName,
                    streamType = command.streamType,
                    previousStreamVersion = 0,
                    currentStreamVersion = command.events.size.toLong(),
                    events = emptyList(),
                ),
            )
        }

        override fun loadStream(streamName: String) = Flux.empty<com.sstt.data.eventstore.model.StoredEvent>()

        override fun readFrom(globalPosition: Long, limit: Int) = Flux.empty<com.sstt.data.eventstore.model.StoredEvent>()
    }

    private fun uuid(value: Long): UUID {
        return UUID.fromString("00000000-0000-0000-0000-${value.toString(16).padStart(12, '0')}")
    }
}
