package com.sstt.data.identity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import com.sstt.data.identity.events.IdentityEventTypes
import com.sstt.data.identity.model.ChangeStudentProfileCommand
import com.sstt.data.identity.model.RegisterStudentCommand
import reactor.core.publisher.Mono

/**
 * Event-sourced write adapter for student identity changes.
 *
 * This class does not write projection tables directly. It translates identity
 * commands into generic JSON events and appends them to the student's stream.
 * Projection handlers are responsible for building queryable read models from
 * those events.
 */
class StudentEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun register(command: RegisterStudentCommand): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(command.studentId),
                streamType = STREAM_TYPE,
                expectedVersion = ExpectedVersion.NoStream,
                events = listOf(
                    NewEvent(
                        eventId = command.eventId,
                        eventType = IdentityEventTypes.STUDENT_REGISTERED,
                        eventVersion = 1,
                        payload = objectMapper.valueToTree(
                            mapOf(
                                "student_id" to command.studentId,
                                "full_name" to command.fullName,
                                "email" to command.email,
                                "group_name" to command.groupName,
                            ),
                        ),
                        metadata = objectMapper.createObjectNode(),
                    ),
                ),
            ),
        )
    }

    fun changeProfile(command: ChangeStudentProfileCommand): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName(command.studentId),
                streamType = STREAM_TYPE,
                expectedVersion = ExpectedVersion.Exact(command.expectedStreamVersion),
                events = listOf(
                    NewEvent(
                        eventId = command.eventId,
                        eventType = IdentityEventTypes.STUDENT_PROFILE_CHANGED,
                        eventVersion = 1,
                        payload = objectMapper.valueToTree(
                            mapOf(
                                "student_id" to command.studentId,
                                "full_name" to command.fullName,
                                "email" to command.email,
                                "group_name" to command.groupName,
                            ),
                        ),
                        metadata = objectMapper.createObjectNode(),
                    ),
                ),
            ),
        )
    }

    companion object {
        const val STREAM_TYPE = "student"

        fun streamName(studentId: Any): String = "student:$studentId"
    }
}
