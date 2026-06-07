package com.sstt.data.academics

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.academics.events.AcademicsEventTypes
import com.sstt.data.academics.model.ChangeTeacherProfileCommand
import com.sstt.data.academics.model.CreateSemesterCommand
import com.sstt.data.academics.model.CreateSubjectCommand
import com.sstt.data.academics.model.CreateTeacherCommand
import com.sstt.data.academics.model.RenameSubjectCommand
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.AppendEventsResult
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
import reactor.core.publisher.Mono

class AcademicsEventWriter(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
) {
    fun createSubject(command: CreateSubjectCommand): Mono<AppendEventsResult> {
        return appendNew(
            streamName = subjectStream(command.subjectId),
            streamType = SUBJECT_STREAM_TYPE,
            eventId = command.eventId,
            eventType = AcademicsEventTypes.SUBJECT_CREATED,
            payload = mapOf("subject_id" to command.subjectId, "name" to command.name),
        )
    }

    fun renameSubject(command: RenameSubjectCommand): Mono<AppendEventsResult> {
        return appendExisting(
            streamName = subjectStream(command.subjectId),
            streamType = SUBJECT_STREAM_TYPE,
            expectedStreamVersion = command.expectedStreamVersion,
            eventId = command.eventId,
            eventType = AcademicsEventTypes.SUBJECT_RENAMED,
            payload = mapOf("subject_id" to command.subjectId, "name" to command.name),
        )
    }

    fun createTeacher(command: CreateTeacherCommand): Mono<AppendEventsResult> {
        return appendNew(
            streamName = teacherStream(command.teacherId),
            streamType = TEACHER_STREAM_TYPE,
            eventId = command.eventId,
            eventType = AcademicsEventTypes.TEACHER_CREATED,
            payload = mapOf("teacher_id" to command.teacherId, "full_name" to command.fullName, "email" to command.email),
        )
    }

    fun changeTeacherProfile(command: ChangeTeacherProfileCommand): Mono<AppendEventsResult> {
        return appendExisting(
            streamName = teacherStream(command.teacherId),
            streamType = TEACHER_STREAM_TYPE,
            expectedStreamVersion = command.expectedStreamVersion,
            eventId = command.eventId,
            eventType = AcademicsEventTypes.TEACHER_PROFILE_CHANGED,
            payload = mapOf("teacher_id" to command.teacherId, "full_name" to command.fullName, "email" to command.email),
        )
    }

    fun createSemester(command: CreateSemesterCommand): Mono<AppendEventsResult> {
        return appendNew(
            streamName = semesterStream(command.semesterId),
            streamType = SEMESTER_STREAM_TYPE,
            eventId = command.eventId,
            eventType = AcademicsEventTypes.SEMESTER_CREATED,
            payload = mapOf(
                "semester_id" to command.semesterId,
                "name" to command.name,
                "starts_on" to command.startsOn,
                "ends_on" to command.endsOn,
            ),
        )
    }

    private fun appendNew(
        streamName: String,
        streamType: String,
        eventId: java.util.UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return append(streamName, streamType, ExpectedVersion.NoStream, eventId, eventType, payload)
    }

    private fun appendExisting(
        streamName: String,
        streamType: String,
        expectedStreamVersion: Long,
        eventId: java.util.UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return append(streamName, streamType, ExpectedVersion.Exact(expectedStreamVersion), eventId, eventType, payload)
    }

    private fun append(
        streamName: String,
        streamType: String,
        expectedVersion: ExpectedVersion,
        eventId: java.util.UUID,
        eventType: String,
        payload: Map<String, Any?>,
    ): Mono<AppendEventsResult> {
        return eventStore.append(
            AppendEventsCommand(
                streamName = streamName,
                streamType = streamType,
                expectedVersion = expectedVersion,
                events = listOf(
                    NewEvent(
                        eventId = eventId,
                        eventType = eventType,
                        eventVersion = 1,
                        payload = objectMapper.valueToTree(payload),
                        metadata = objectMapper.createObjectNode(),
                    ),
                ),
            ),
        )
    }

    companion object {
        const val SUBJECT_STREAM_TYPE = "subject"
        const val TEACHER_STREAM_TYPE = "teacher"
        const val SEMESTER_STREAM_TYPE = "semester"

        fun subjectStream(subjectId: Any): String = "subject:$subjectId"
        fun teacherStream(teacherId: Any): String = "teacher:$teacherId"
        fun semesterStream(semesterId: Any): String = "semester:$semesterId"
    }
}
