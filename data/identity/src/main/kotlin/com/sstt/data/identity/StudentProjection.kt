package com.sstt.data.identity

import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.projection.EventProjection
import com.sstt.data.identity.events.IdentityEventTypes
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

/**
 * Projection handler that builds `projections.students` from identity events.
 *
 * It is intentionally small and stateless. A projection runner can feed it
 * stored events in global position order and persist runner offsets separately
 * through the eventstore projection offset port.
 */
class StudentProjection(
    private val databaseClient: DatabaseClient,
) : EventProjection {
    override val name: String = PROJECTION_NAME

    override fun apply(event: StoredEvent): Mono<Void> {
        return when (event.eventType) {
            IdentityEventTypes.STUDENT_REGISTERED -> applyStudentRegistered(event)
            IdentityEventTypes.STUDENT_PROFILE_CHANGED -> applyStudentProfileChanged(event)
            else -> Mono.empty()
        }
    }

    private fun applyStudentRegistered(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            insert into projections.students (
                student_id,
                full_name,
                email,
                group_name,
                stream_version,
                created_at,
                updated_at
            )
            values (
                :student_id,
                :full_name,
                :email,
                :group_name,
                :stream_version,
                :created_at,
                :updated_at
            )
            on conflict (student_id) do update
            set full_name = excluded.full_name,
                email = excluded.email,
                group_name = excluded.group_name,
                stream_version = excluded.stream_version,
                updated_at = excluded.updated_at
            where projections.students.stream_version < excluded.stream_version
            """.trimIndent(),
        )
            .bindStudentPayload(event)
            .bind("created_at", event.occurredAt)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun applyStudentProfileChanged(event: StoredEvent): Mono<Void> {
        return databaseClient.sql(
            """
            update projections.students
            set full_name = :full_name,
                email = :email,
                group_name = :group_name,
                stream_version = :stream_version,
                updated_at = :updated_at
            where student_id = :student_id
              and stream_version < :stream_version
            """.trimIndent(),
        )
            .bindStudentPayload(event)
            .fetch()
            .rowsUpdated()
            .then()
    }

    private fun DatabaseClient.GenericExecuteSpec.bindStudentPayload(
        event: StoredEvent,
    ): DatabaseClient.GenericExecuteSpec {
        return bind("student_id", UUID.fromString(event.payload["student_id"].asText()))
            .bind("full_name", event.payload["full_name"].asText())
            .bind("email", event.payload["email"].asText())
            .bind("group_name", event.payload["group_name"].asText())
            .bind("stream_version", event.streamVersion)
            .bind("updated_at", event.occurredAt)
    }

    companion object {
        const val PROJECTION_NAME = "projections.students"
    }
}
