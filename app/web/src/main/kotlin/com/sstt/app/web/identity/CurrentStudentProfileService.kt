package com.sstt.app.web.identity

import com.sstt.data.eventstore.projection.ReactiveProjectionRunner
import com.sstt.data.identity.StudentEventWriter
import com.sstt.data.identity.StudentProjection
import com.sstt.data.identity.StudentReadRepository
import com.sstt.data.identity.model.ChangeStudentProfileCommand
import com.sstt.data.identity.model.RegisterStudentCommand
import com.sstt.data.identity.model.StudentRecord
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Application service for the current student's editable profile.
 *
 * The service coordinates the event-sourced write model and the asynchronous
 * read projection for good MVP ergonomics: after writes it explicitly runs the
 * identity projection once so the web response can return the fresh read model.
 */
@Service
class CurrentStudentProfileService(
    private val properties: CurrentStudentProperties,
    private val studentEventWriter: StudentEventWriter,
    private val studentReadRepository: StudentReadRepository,
    private val projectionRunner: ReactiveProjectionRunner,
) {
    fun currentProfile(): Mono<StudentRecord> {
        return catchUpProjection()
            .then(studentReadRepository.findById(properties.id))
            .switchIfEmpty(registerDefaultProfile())
    }

    fun updateProfile(form: ProfileForm): Mono<StudentRecord> {
        return currentProfile()
            .flatMap { current ->
                studentEventWriter.changeProfile(
                    ChangeStudentProfileCommand(
                        studentId = properties.id,
                        eventId = UUID.randomUUID(),
                        expectedStreamVersion = current.streamVersion,
                        fullName = form.fullName,
                        email = form.email,
                        groupName = form.groupName,
                    ),
                )
            }
            .then(catchUpProjection())
            .then(studentReadRepository.findById(properties.id))
    }

    private fun registerDefaultProfile(): Mono<StudentRecord> {
        return studentEventWriter.register(
            RegisterStudentCommand(
                studentId = properties.id,
                eventId = UUID.randomUUID(),
                fullName = properties.defaultFullName,
                email = properties.defaultEmail,
                groupName = properties.defaultGroupName,
            ),
        )
            .then(catchUpProjection())
            .then(studentReadRepository.findById(properties.id))
            .onErrorResume { failure ->
                catchUpProjection()
                    .then(studentReadRepository.findById(properties.id))
                    .switchIfEmpty(Mono.error(failure))
            }
    }

    private fun catchUpProjection(): Mono<Void> {
        return projectionRunner.runOnce(StudentProjection.PROJECTION_NAME).then()
    }
}
